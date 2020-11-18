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

import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseTree;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TermExtractionEngineTest {
  private static final Logger LOG = LoggerFactory.getLogger(TermExtractionEngineTest.class);

  @Test
  public void testGetExpansionStrings() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    String sessionId = "test";

    InputStream configurationInputStream = getClass().getResourceAsStream("termTestCONLL.txt");
    Reader configurationReader = new BufferedReader(new InputStreamReader(configurationInputStream, "UTF-8"));

    Config config = ConfigFactory.load();
    Config readerConfig = config.getConfig("talismane.core." + sessionId + ".parser.input");
    ParserAnnotatedCorpusReader corpusReader = new ParserRegexBasedCorpusReader(configurationReader, readerConfig, sessionId);

    ParseConfiguration configuration = corpusReader.nextConfiguration();
    LOG.debug(configuration.toString());

    ParseTree parseTree = new ParseTree(configuration, true);
    LOG.debug(parseTree.toString());

    final TerminologyBase terminologyBase = mock(TerminologyBase.class);
    final Term term = mock(Term.class);
    when(terminologyBase.findTerm(anyString())).thenReturn(term);
    
    PosTaggedToken chat = configuration.getPosTagSequence().get(3);
    assertEquals("chat", chat.getToken().getText());

    // test depth (1)
    TermExtractionEngine engine = new TermExtractionEngine(sessionId, 1);
    Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<>();
    List<Expansion> expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    Set<String> expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }

    LOG.debug("All expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }
    String[] limitedDepthExpansionArray = new String[] { "chat" };
    Set<String> limitedDepthExpansions = new TreeSet<>();
    for (String expansion : limitedDepthExpansionArray)
      limitedDepthExpansions.add(expansion);

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }
    assertEquals(limitedDepthExpansions, expansionStrings);

    // test depth (2)
    engine = new TermExtractionEngine(sessionId, 2);
    expansionsPerNoun = new HashMap<>();
    expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }

    String[] depth2Array = new String[] { "petit chat", "chat noir", "petit chat noir", "chat noir et blanc", "petit chat noir et blanc",
        "chat noir et blanc de la grand-mère", "petit chat noir et blanc de la grand-mère" };

    for (String expansion : depth2Array)
      limitedDepthExpansions.add(expansion);

    LOG.debug("Expected expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : limitedDepthExpansions) {
      LOG.debug(expansionString);
    }

    LOG.debug("Actual expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }

    LOG.debug("Parents and children depth " + engine.getMaxDepth());
    boolean foundParent1 = false;
    boolean foundParent2 = false;
    boolean foundChild1 = false;
    for (Expansion expansion : expansions) {
      LOG.debug("# " + expansion.display());
      for (Expansion parent : expansion.getParents()) {
        LOG.debug("Parent: " + parent.display());
      }
      for (Expansion child : expansion.getChildren()) {
        LOG.debug("Child: " + child.display());
      }

      if (expansion.display().equals("petit chat noir et blanc de la grand-mère")) {
        for (Expansion parent : expansion.getParents()) {
          if (parent.display().equals("chat noir et blanc de la grand-mère")) {
            foundParent1 = true;
          } else if (parent.display().equals("petit chat noir et blanc")) {
            foundParent2 = true;
          }
        }
        for (Expansion child : expansion.getChildren()) {
          if (child.display().equals("grand-mère")) {
            foundChild1 = true;
          }
        }
      }
    }
    assertTrue("Didn't find parent1", foundParent1);
    assertTrue("Didn't find parent2", foundParent2);
    assertTrue("Didn't find child1", foundChild1);

    assertEquals(limitedDepthExpansions, expansionStrings);

    // depth test (3)
    engine = new TermExtractionEngine(sessionId, 3);
    String[] depth3Additions = new String[] { " maternelle", };

    List<String> depth3Expansions = new ArrayList<>();
    for (String depth2Expansion : depth2Array) {
      if (depth2Expansion.endsWith("grand-mère")) {
        for (String depth3Addition : depth3Additions) {
          String newExpansion = depth2Expansion + depth3Addition;
          depth3Expansions.add(newExpansion);
        }
      }
    }
    limitedDepthExpansions.addAll(depth3Expansions);
    LOG.debug("Expected expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : limitedDepthExpansions) {
      LOG.debug(expansionString);
    }

    expansionsPerNoun = new HashMap<>();
    expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }
    LOG.debug("Actual expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }

    LOG.debug("Parents and children depth " + engine.getMaxDepth());
    for (Expansion expansion : expansions) {
      LOG.debug("# " + expansion.display());
      for (Expansion parent : expansion.getParents()) {
        LOG.debug("Parent: " + parent.display());
      }
      for (Expansion child : expansion.getChildren()) {
        LOG.debug("Child: " + child.display());
      }
    }

    assertEquals(limitedDepthExpansions, expansionStrings);

    // depth test (4)
    engine = new TermExtractionEngine(sessionId, 4);
    String[] depth4Additions = new String[] { " de sa deuxième femme" };

    List<String> depth4Expansions = new ArrayList<>();
    for (String depth3Expansion : depth3Expansions) {
      if (depth3Expansion.endsWith("maternelle")) {
        for (String depth4Addition : depth4Additions) {
          String newExpansion = depth3Expansion + depth4Addition;
          depth4Expansions.add(newExpansion);
        }
      }
    }
    limitedDepthExpansions.addAll(depth4Expansions);

    LOG.debug("Expected expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : limitedDepthExpansions) {
      LOG.debug(expansionString);
    }

    expansionsPerNoun = new HashMap<>();
    expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }

    LOG.debug("Actual expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }

    LOG.debug("Parents and children depth " + engine.getMaxDepth());
    for (Expansion expansion : expansions) {
      LOG.debug("# " + expansion.display());
      for (Expansion parent : expansion.getParents()) {
        LOG.debug("Parent: " + parent.display());
      }
      for (Expansion child : expansion.getChildren()) {
        LOG.debug("Child: " + child.display());
      }
    }
    assertEquals(limitedDepthExpansions, expansionStrings);
  }

  @Test
  public void testGetPluralStrings() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    String sessionId = "test";
    
    InputStream configurationInputStream = getClass().getResourceAsStream("termTestCONLLPlural.txt");
    Reader configurationReader = new BufferedReader(new InputStreamReader(configurationInputStream, "UTF-8"));

    Config config = ConfigFactory.load();
    Config readerConfig = config.getConfig("talismane.core." + sessionId + ".parser.input");
    ParserAnnotatedCorpusReader corpusReader = new ParserRegexBasedCorpusReader(configurationReader, readerConfig, sessionId);

    ParseConfiguration configuration = corpusReader.nextConfiguration();
    LOG.debug(configuration.toString());

    final TerminologyBase terminologyBase = mock(TerminologyBase.class);
    final Term term = mock(Term.class);
    when(terminologyBase.findTerm(anyString())).thenReturn(term);
    
    PosTaggedToken chat = configuration.getPosTagSequence().get(3);
    assertEquals("chats", chat.getToken().getText());

    // test depth (1)
    TermExtractionEngine engine = new TermExtractionEngine(sessionId, 1);
    Map<PosTaggedToken, List<Expansion>> expansionsPerNoun = new HashMap<>();
    List<Expansion> expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    Set<String> expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }

    LOG.debug("All expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }
    String[] limitedDepthExpansionArray = new String[] { "chat" };
    Set<String> limitedDepthExpansions = new TreeSet<>();
    for (String expansion : limitedDepthExpansionArray)
      limitedDepthExpansions.add(expansion);

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }
    assertEquals(limitedDepthExpansions, expansionStrings);

    // test depth (2)
    engine = new TermExtractionEngine(sessionId, 2);
    expansionsPerNoun = new HashMap<>();
    expansions = engine.getExpansions(chat, configuration, 0, expansionsPerNoun);
    expansionStrings = new TreeSet<>();
    for (Expansion expansion : expansions) {
      expansionStrings.add(expansion.display());
    }

    String[] depth2Array = new String[] { "petit chat", "chat noir", "petit chat noir", "chat noir et blanc", "petit chat noir et blanc",
        "chat noir et blanc de ses grand-mères", "petit chat noir et blanc de ses grand-mères" };

    for (String expansion : depth2Array)
      limitedDepthExpansions.add(expansion);

    LOG.debug("Expected expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : limitedDepthExpansions) {
      LOG.debug(expansionString);
    }

    LOG.debug("Actual expansions depth " + engine.getMaxDepth() + ":");
    for (String expansionString : expansionStrings) {
      LOG.debug(expansionString);
    }

    for (String expansion : limitedDepthExpansions) {
      assertTrue("Missing expansion: " + expansion, expansionStrings.contains(expansion));
    }

    LOG.debug("Parents and children depth " + engine.getMaxDepth());
    boolean foundParent1 = false;
    boolean foundParent2 = false;
    boolean foundChild1 = false;
    for (Expansion expansion : expansions) {
      LOG.debug("# " + expansion.display());
      for (Expansion parent : expansion.getParents()) {
        LOG.debug("Parent: " + parent.display());
      }
      for (Expansion child : expansion.getChildren()) {
        LOG.debug("Child: " + child.display());
      }

      if (expansion.display().equals("petit chat noir et blanc de ses grand-mères")) {
        for (Expansion parent : expansion.getParents()) {
          if (parent.display().equals("chat noir et blanc de ses grand-mères")) {
            foundParent1 = true;
          } else if (parent.display().equals("petit chat noir et blanc")) {
            foundParent2 = true;
          }
        }
        for (Expansion child : expansion.getChildren()) {
          if (child.display().equals("grand-mère")) {
            foundChild1 = true;
          }
        }
      }
    }
    assertTrue("Didn't find parent1", foundParent1);
    assertTrue("Didn't find parent2", foundParent2);
    assertTrue("Didn't find child1", foundChild1);

    assertEquals(limitedDepthExpansions, expansionStrings);
  }
}
