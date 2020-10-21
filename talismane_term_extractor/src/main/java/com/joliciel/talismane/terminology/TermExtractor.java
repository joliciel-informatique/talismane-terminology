///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.terminology;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.terminology.postgres.PostGresTerminologyBase;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.filters.UppercaseSeriesFilter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Extracts all noun phrases from a given parse configuration.
 * 
 * To function correctly, the input has to include location information.
 * 
 * @author Assaf Urieli
 *
 */
public class TermExtractor implements ParseConfigurationProcessor {

  @Override
  public void close() throws IOException {

  }

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TermExtractor.class);
  
  private final int maxDepth;
  private static final int TOKEN_BUFFER_FOR_CONTEXT = 6;

  private final String sessionId;

  private final TerminologyBase terminologyBase;

  private final List<TermObserver> termObservers;
  
  private final Set<String> nominalTags;

  private final TermExtractionEngine engine;
  
  public TermExtractor(String sessionId) throws ReflectiveOperationException {
    this(sessionId, ConfigFactory.load().getConfig("talismane.terminology").getInt("max-depth"));
  }

  TermExtractor(String sessionId, int maxDepth) throws ReflectiveOperationException {
    this.sessionId = sessionId;

    Config config = ConfigFactory.load().getConfig("talismane.terminology");
    String projectCode = config.getString("project-code");
    this.terminologyBase = new PostGresTerminologyBase(projectCode);

    String language = config.getString("language");
    Config languageConfig = config.getConfig(language);

    this.nominalTags = new HashSet<>(languageConfig.getStringList("nominal-tags"));

    this.maxDepth = maxDepth;

    this.termObservers = TermObserver.getObservers();
    
    this.engine = new TermExtractionEngine(sessionId, maxDepth);
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws TalismaneException {
    for (TermObserver termObserver : termObservers) {
      termObserver.onNewContext(parseConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().toString());
    }

    // find all nouns
    List<PosTaggedToken> nouns = new ArrayList<PosTaggedToken>();
    for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
      if (nominalTags.contains(posTaggedToken.getTag().getCode())) {
        nouns.add(posTaggedToken);
      }
    }

    Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<PosTaggedToken, List<Expansion>>();
    for (PosTaggedToken noun : nouns) {
      this.getExpansionStrings(noun, parseConfiguration, 0, expansionsPerNoun);
    } // next noun head
  }

  @Override
  public void onCompleteParse() throws IOException {
    // nothing to do
  }

  Set<String> getExpansionStrings(PosTaggedToken noun, ParseConfiguration parseConfiguration, int depth,
      Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
    Set<String> nounPhrases = null;
    List<Expansion> expansions = this.engine.getExpansions(noun, parseConfiguration, 0, expansionsPerNoun);

    nounPhrases = new TreeSet<String>();

    for (Expansion expansion : expansions) {
      if (expansion.display().length() == 0)
        continue;

      DependencyNode node = expansion.getNode();
      Term term = terminologyBase.findTerm(expansion.display());
      if (term.isNew()) {
        term.setLexicalWordCount(expansion.getLexicalWordCount());
        term.save();
      }

      Token firstToken = node.getFirstToken().getToken();

      Context context = terminologyBase.findContext(term, firstToken.getFileName(), firstToken.getLineNumber(), firstToken.getColumnNumber());

      if (context.isNew()) {
        Token lastToken = node.getLastToken().getToken();

        context.setEndLineNumber(lastToken.getLineNumberEnd());
        context.setEndColumnNumber(lastToken.getColumnNumberEnd());

        int startIndex = firstToken.getIndex();
        startIndex -= TOKEN_BUFFER_FOR_CONTEXT;
        if (startIndex < 0)
          startIndex = 0;

        int endIndex = lastToken.getIndex();
        endIndex += TOKEN_BUFFER_FOR_CONTEXT;
        if (endIndex >= parseConfiguration.getPosTagSequence().getTokenSequence().size())
          endIndex = parseConfiguration.getPosTagSequence().getTokenSequence().size() - 1;

        Token startToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(startIndex);
        Token endToken = parseConfiguration.getPosTagSequence().getTokenSequence().get(endIndex);
        String text = parseConfiguration.getPosTagSequence().getTokenSequence().getSentence().getText().toString();
        String textSegment = text.substring(startToken.getStartIndex(), endToken.getEndIndex());
        context.setTextSegment(textSegment);
        context.save();
      }

      term.addContext(context);

      for (TermObserver termObserver : termObservers) {
        termObserver.onNewTerm(term);
      }

      for (Expansion parent : expansion.getParents()) {
        if (parent.display().length() == 0)
          continue;
        Term parentTerm = terminologyBase.findTerm(parent.display());
        if (parentTerm.isNew()) {
          parentTerm.setLexicalWordCount(parent.getLexicalWordCount());
        }
        parentTerm.addExpansion(term);
        parentTerm.save();
      }

      for (Expansion child : expansion.getChildren()) {
        if (child.display().length() == 0)
          continue;
        Term childTerm = terminologyBase.findTerm(child.display());
        if (childTerm.isNew()) {
          childTerm.setLexicalWordCount(child.getLexicalWordCount());
        }
        childTerm.addHead(term);
        childTerm.save();
      }

      terminologyBase.commit();

      String nounPhrase = expansion.display();
      nounPhrases.add(nounPhrase);
    }
    return nounPhrases;
  }
}
