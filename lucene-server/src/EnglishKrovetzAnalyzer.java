import java.io.Reader;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishKrovetzAnalyzer extends StopwordAnalyzerBase{
	  private final CharArraySet stemExclusionSet;
	   
	  /**
	   * Returns an unmodifiable instance of the default stop words set.
	   * @return default stop words set.
	   */
	  public static CharArraySet getDefaultStopSet(){
	    return DefaultSetHolder.DEFAULT_STOP_SET;
	  }
	  
	  /**
	   * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class 
	   * accesses the static final set the first time.;
	   */
	  private static class DefaultSetHolder {
	    static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
	  }

	  /**
	   * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
	   */
	  public EnglishKrovetzAnalyzer() {
	    this(DefaultSetHolder.DEFAULT_STOP_SET);
	  }
	  
	  /**
	   * Builds an analyzer with the given stop words.
	   * 
	   * @param stopwords a stopword set
	   */
	  public EnglishKrovetzAnalyzer(CharArraySet stopwords) {
	    this(stopwords, CharArraySet.EMPTY_SET);
	  }

	  /**
	   * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
	   * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
	   * stemming.
	   * 
	   * @param stopwords a stopword set
	   * @param stemExclusionSet a set of terms not to be stemmed
	   */
	  public EnglishKrovetzAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
	    super(stopwords);
	    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	  }

	  /**
	   * Creates a
	   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	   * which tokenizes all the text in the provided {@link Reader}.
	   * 
	   * @return A
	   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	   *         built from an {@link StandardTokenizer} filtered with
	   *         {@link StandardFilter}, {@link EnglishPossessiveFilter}, 
	   *         {@link LowerCaseFilter}, {@link StopFilter}
	   *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
	   *         provided and {@link PorterStemFilter}.
	   */
	  @Override
	  protected TokenStreamComponents createComponents(String fieldName) {
	    final Tokenizer source = new StandardTokenizer();
	    TokenStream result = new StandardFilter(source);
	    result = new EnglishPossessiveFilter(result);
	    result = new LowerCaseFilter(result);
	    result = new StopFilter(result, stopwords);
	    if(!stemExclusionSet.isEmpty())
	      result = new SetKeywordMarkerFilter(result, stemExclusionSet);
	    result = new KStemFilter(result);
	    return new TokenStreamComponents(source, result);
	  }

	  @Override
	  protected TokenStream normalize(String fieldName, TokenStream in) {
	    TokenStream result = new StandardFilter(in);
	    result = new LowerCaseFilter(result);
	    return result;
	  }
}
