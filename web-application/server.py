import os
import json
import time
import sys

from flask import Flask, Response, send_file, request, render_template
from py4j.java_gateway import JavaGateway, GatewayParameters
from flask_cors import CORS

from search_engine_interface import SearchEngineInterface
from web_utils import WebUtils
from ml_server import MLServer

app = Flask(__name__)
CORS(app)


"""This Application back-end Flask server"""


@app.route('/initial/', methods=['GET', 'POST'])
def initialize_page():
    """Initializing the html of the application with the different menus."""
    tore_turn = {'fields_menu': fields_menu, 'field_names': field_names, 'field_weights': initial_field_weights,
                 'collection_menu': collection_menu, 'collection_names': collection_names,
                 'query_examples_1': query_examples_1, 'query_examples_2': query_examples_2, 'model_names': model_names,
                 'top_menu': top_menu}
    response = Response(response=json.dumps(tore_turn), status=200, mimetype='application/json')
    return response


@app.route('/search/', methods=['GET', 'POST'])
def search():
    """Performing a figure search using a keyword query."""
    json_data = request.get_data(as_text=True)
    data = json.loads(json_data)
    query = data['query']
    ip = data['ip']
    collection = data['collection']
    page_id = int(data['page'])
    example_figure = data['example_figure']
    model = data['model']
    field_weights = {field: float(data['field_weights'][field]) for field in data['field_weights']}
    time_stamp = str(time.time())

    normalizer = sum(field_weights.values())
    for field in field_weights:
        field_weights[field] /= float(normalizer)

    field_weights_java = server.jvm.java.util.HashMap()
    for field in field_weights:
        field_weights_java[field] = field_weights[field]

    search_configuration = ''
    for field in field_weights:
        search_configuration += str(field) + str(field_weights[field]) + '_'
    configuration = search_configuration.rstrip('_')

    search_log_line = 'search_event' + ',' + ip + ',' + time_stamp + ',' + query.replace(',', 'commasign') + ','
    search_log_line += configuration + ',' + str(page_id) + ',' + collection + ',' + model
    search_log_line += ',' + example_figure
    print(search_log_line)
    sys.stdout.flush()

    results, engine = sei.search(query, collection, model, field_weights_java)

    captions = []
    mentions = []
    urls = []
    ids = []
    files = []
    related_flags = {related_type: [] for related_type in ml_server.related_figures[collection]}
    paper_flags = []
    figure_flags = []

    fields_list = server.jvm.java.util.ArrayList()
    fields_list.append(web_utils.CAPTION_FIELD)
    fields_list.append(web_utils.FIGURE_FIELD)
    fields_list.append(web_utils.PAPER_FIELD)
    fields_list.append(web_utils.FILE_FIELD)
    fields_list.append(web_utils.SNIPPET_FIELD)
    fields_list.append(web_utils.TITLE_FIELD)
    fields_list.append(web_utils.ABSTRACT_FIELD)

    docs_content = engine.getResultListContent(results, fields_list)
    doc_mapping = {}

    if example_figure != 'none':
        example_figure = example_figure.split('\'')[1] + '_' + example_figure.split('\'')[2]
        embeddings_similarity_map = {}
        baseline_similarity_map = {}
        for i, single_dict in enumerate(docs_content):
            if i == 100:
                break
            doc_mapping[i] = single_dict

            other_figure = single_dict[web_utils.PAPER_FIELD] + '_' + single_dict[web_utils.FIGURE_FIELD]
            embeddings_similarity_map[i] = ml_server.get_embeddings_similarity(example_figure, other_figure, collection)
            baseline_similarity_map[i] = float(single_dict[web_utils.SCORE_FIELD])

        min_embeddings = min(embeddings_similarity_map.values())
        max_embeddings = max(embeddings_similarity_map.values())
        min_baseline = min(baseline_similarity_map.values())
        max_baseline = max(baseline_similarity_map.values())

        for i in embeddings_similarity_map:
            image_norm = max_embeddings - min_embeddings
            if image_norm == 0:
                image_norm = 1
            text_norm = max_baseline - min_baseline
            if text_norm == 0:
                text_norm = 1

            embeddings_similarity_map[i] = (embeddings_similarity_map[i] - min_embeddings) / image_norm
            baseline_similarity_map[i] = (baseline_similarity_map[i] - min_baseline) / text_norm

        final_ranking = {}
        for i in embeddings_similarity_map:
            final_ranking[i] = 0.9 * embeddings_similarity_map[i] + 0.1 * baseline_similarity_map[i]
        docs_content = []
        sorted_list = sorted(final_ranking, key=final_ranking.get, reverse=True)

        for key in sorted_list:
            docs_content.append(doc_mapping[key])

    for single_dict in docs_content[page_id*10:page_id*10+10]:
        figure_num = single_dict[web_utils.FIGURE_FIELD]
        mention = sei.boldface_mention(single_dict[web_utils.SNIPPET_FIELD], query, figure_num)
        caption = single_dict[web_utils.CAPTION_FIELD]

        if not os.path.isfile(web_utils.configurations_dict[web_utils.IMAGE_FILE_DIR][0][0] + '/' + single_dict['file']):
            file_dir = web_utils.configurations_dict[web_utils.APP_URL][0][0] + 'image/default.png'
        else:
            file_dir = web_utils.configurations_dict[web_utils.APP_URL][0][0] + 'image/' + single_dict['file']

        if not sei.check_duplications(mention, caption, mentions, captions):
            continue

        figure_id = single_dict[web_utils.PAPER_FIELD] + '_' + single_dict[web_utils.FIGURE_FIELD]
        captions += [sei.process_caption(caption)]
        mentions += [sei.trim_snippet(mention)]
        urls += [sei.get_pdf_url(single_dict[web_utils.PAPER_FIELD])]
        files += [file_dir]
        ids += ['figure_id' + str(single_dict[web_utils.PAPER_FIELD].encode('utf-8')).replace(' ', '') + str(figure_num)]

        figure_flag = '1'
        figure_ids = sei.same_article_figures.get(collection, []).get(figure_id, [])
        figure_ids_java = server.jvm.java.util.ArrayList()
        for fig_id in figure_ids:
            figure_ids_java.append(fig_id)
        figure_content = sei.get_figures_content(figure_ids_java, collection, model)
        if len(figure_content) == 0:
            figure_flag = '0'
        figure_flags += [figure_flag]

        if len(str(single_dict[web_utils.TITLE_FIELD])) < 5 and len(str(single_dict[web_utils.ABSTRACT_FIELD])) < 5:
            paper_flags += ['0']
        else:
            paper_flags += ['1']

        for related_type in ml_server.related_figures[collection]:
            related_flag = '1'
            figure_ids = ml_server.related_figures[collection][related_type].get(figure_id, [])
            figure_ids_java = server.jvm.java.util.ArrayList()
            for fig_id in figure_ids:
                figure_ids_java.append(fig_id)
            figure_content = sei.get_figures_content(figure_ids_java, collection, model)
            if len(figure_content) == 0:
                related_flag = '0'
            related_flags[related_type] = related_flags[related_type] + [related_flag]

    tore_turn = {'captions': captions, 'mentions': mentions, 'urls': urls, 'files': files, 'ids': ids,
                 'paper': paper_flags, 'figures': figure_flags}

    for related_type in related_flags:
        tore_turn['related_' + related_type] = related_flags[related_type]

    response = Response(response=json.dumps(tore_turn), status=200, mimetype='application/json')
    return response


@app.route('/image/<name>', methods=['GET', 'POST'])
def image(name):
    """Sending an image files to the client."""
    return send_file(web_utils.configurations_dict[web_utils.IMAGE_FILE_DIR][0][0] + '/' + name)


@app.route('/static/<name>', methods=['GET', 'POST'])
def style(name):
    """Sending CSS files to the client."""
    return send_file('/home/skuzi2/fig-search-acl/static/' + name)


@app.route('/button_log/', methods=['GET', 'POST'])
def relevance_button_log():
    """Logging information about clicking a relevance judgment button."""
    json_data = request.get_data(as_text=True)
    data = json.loads(json_data)
    query = data['query']
    args = data['value'].split('_')
    collection = data['collection']
    val = '_'.join(args[0:len(args)-1])
    rank = args[len(args)-1]
    ip = data['ip']
    model = data['model']
    field_weights = data['field_weights']
    time_stamp = str(time.time())

    search_configuration = ''
    for field in field_weights:
        search_configuration += str(field) + str(field_weights[field]) + '_'
    configuration = search_configuration.rstrip('_')

    print('relevance_button_event' + ',' + ip + ',' + time_stamp + ',' + query.replace(',','commasign') + ','
          + configuration + ',' + val + ',' + rank + ',' + collection + ',' + model)
    sys.stdout.flush()
    return ''


@app.route('/url_log/', methods=['GET', 'POST'])
def url_log():
    """Logging information about clicking a url."""
    json_data = request.get_data(as_text=True)
    data = json.loads(json_data)
    query = data['query']
    url = data['value']
    ip = data['ip']
    rank = data['rank']
    collection = data['collection']
    model = data['model']
    field_weights = data['field_weights']
    time_stamp = str(time.time())

    search_configuration = ''
    for field in field_weights:
        search_configuration += str(field) + str(field_weights[field]) + '_'
    configuration = search_configuration.rstrip('_')

    print('url_event' + ',' + ip + ',' + time_stamp + ',' + query.replace(',','commasign') + ',' + configuration +
          ',' + url + ',' + rank + ',' + collection + ',' + model)
    sys.stdout.flush()
    return ''


@app.route('/pdf/<name>', methods=['GET', 'POST'])
def pdf(name):
    """Sending a pdf file to the server."""
    return send_file(web_utils.configurations_dict.get(web_utils.PDF_DIR, [('pdfs',)])[0][0] + '/' + name)


@app.route('/')
def index():
    """Sending the main page of the application."""
    return render_template('index.html', server_url_input=server_url)


@app.route('/about/')
def about():
    """Sending the "about" page of the application."""
    return render_template('about.html', server_url_input=server_url)


@app.route('/related/', methods=['GET', 'POST'])
def related():
    """Sending the related figures of a target figure."""
    json_data = request.get_data(as_text=True)
    data = json.loads(json_data)
    figure_id = data['figure_id']
    related_type = figure_id.split('figure_idb')[0].lstrip('_related')
    args = figure_id.split('\'')
    figure_id = args[1] + '_' + args[2]
    collection = data['collection']
    model = data['model']
    captions = []
    files = []
    urls = []
    ids = []

    figure_ids = ml_server.related_figures[collection][related_type].get(figure_id, [])
    figure_ids_java = server.jvm.java.util.ArrayList()
    for id in figure_ids:
        figure_ids_java.append(id)
    figure_content = sei.get_figures_content(figure_ids_java, collection, model)

    for figure_dict in figure_content:
        captions.append(sei.process_caption(figure_dict[web_utils.CAPTION_FIELD]))
        file_dir = web_utils.configurations_dict[web_utils.APP_URL][0][0] + 'image/' + figure_dict['file']
        if not os.path.isfile(web_utils.configurations_dict[web_utils.IMAGE_FILE_DIR][0][0] + figure_dict['file']):
            file_dir = web_utils.configurations_dict[web_utils.APP_URL][0][0] + 'image/default.png'
        files.append(file_dir)
        urls += [sei.get_pdf_url(figure_dict['paper'])]
        ids += ['figureidb\'' + figure_dict['paper'] + '\'' + figure_dict['figure']]
    tore_turn = {'captions': captions, 'files': files, 'urls': urls, 'ids': ids}
    response = Response(response=json.dumps(tore_turn), status=200, mimetype='application/json')
    return response


@app.route('/paper/', methods=['GET', 'POST'])
def paper():
    """Sending the article information for a figure."""
    json_data = request.get_data(as_text=True)
    data = json.loads(json_data)
    paper_id = data['figure_id'].split('\'')[1]
    figure_id = data['figure_id'].split('\'')[2]
    collection = data['collection']
    model = data['model']

    figure_ids_java = server.jvm.java.util.ArrayList()
    figure_ids_java.append(paper_id + '_' + figure_id)
    figure_content = sei.get_figures_content(figure_ids_java, collection, model)

    title = ''
    abstract = ''
    url = ''

    for figure_dict in figure_content:
        title = figure_dict[web_utils.TITLE_FIELD]
        abstract = figure_dict[web_utils.ABSTRACT_FIELD]
        url = sei.get_pdf_url(figure_dict[web_utils.PAPER_FIELD])

    tore_turn = {'title': title, 'abstract': abstract, 'url': url}
    response = Response(response=json.dumps(tore_turn), status=200, mimetype='application/json')

    return response


if __name__ == '__main__':

    web_utils = WebUtils('../fig.explorer.config')

    fields_menu, field_names, initial_field_weights, model_names = web_utils.build_settings_menu()
    collection_menu, collection_names, query_examples_1, query_examples_2 = web_utils.build_collection_menu()
    top_menu = web_utils.build_top_menu()

    web_port_number = int(web_utils.configurations_dict.get(web_utils.WEB_APP_PORT, [('5000',)])[0][0])
    lucene_port_number = int(web_utils.configurations_dict.get(web_utils.LUCENE_PORT_NUM, [('25000',)])[0][0])
    host_name = web_utils.configurations_dict.get(web_utils.WEB_APP_HOST, [('0.0.0.0',)])[0][0]
    default_server_url = 'http://' + web_utils.configurations_dict.get(web_utils.WEB_APP_HOST, [('0.0.0.0',)])[0][0]
    default_server_url += ':' + web_utils.configurations_dict.get(web_utils.WEB_APP_PORT, [('5000',)])[0][0] + '/'
    server_url = web_utils.configurations_dict.get(web_utils.APP_URL, [(default_server_url,)])[0][0]

    server = JavaGateway(gateway_parameters=GatewayParameters(port=lucene_port_number))
    sei = SearchEngineInterface(server, web_utils)
    ml_server = MLServer(web_utils, sei)

    if not os.path.exists('logs'): os.mkdir('logs')
    sys.stdout = open('logs/fig-search.' + str(time.time()) + '.log', 'w')

    app.run(host=host_name, port=web_port_number)