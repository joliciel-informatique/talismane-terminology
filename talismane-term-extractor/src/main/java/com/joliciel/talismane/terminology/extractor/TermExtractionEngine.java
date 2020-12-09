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
package com.joliciel.talismane.terminology.extractor;

import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.terminology.Expansion;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Extracts all noun phrases from a given parse configuration.
 *
 * To function correctly, the input has to include location information.
 *
 * @author Assaf Urieli
 *
 */
public class TermExtractionEngine {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TermExtractor.class);

  private final int maxDepth;
  
  private final String sessionId;

  private final Set<String> zeroDepthLabels;
  private final Set<String> termStopTags;
  private final Set<String> nonStandaloneTags;
  private final Set<String> nonStandaloneIfHasDependents;
  private final Set<String> nonTopLevelLabels;

  
  TermExtractionEngine(String sessionId, int maxDepth) throws ReflectiveOperationException {
    this.sessionId = sessionId;

    Config config = ConfigFactory.load().getConfig("talismane.terminology");

    String language = config.getString("language");
    Config languageConfig = config.getConfig(language);
    
    this.nonStandaloneIfHasDependents = new HashSet<>(languageConfig.getStringList("non-standalone-if-has-dependents"));
    this.nonStandaloneTags = new HashSet<>(languageConfig.getStringList("non-standalone-tags"));
    this.nonTopLevelLabels = new HashSet<>(languageConfig.getStringList("non-top-level-labels"));
    this.termStopTags = new HashSet<>(languageConfig.getStringList("term-stop-tags"));
    this.zeroDepthLabels = new HashSet<>(languageConfig.getStringList("zero-depth-labels"));

    this.maxDepth = maxDepth;
  }

  /**
   * Get all expansions for this node recursively. Note: we assume in here
   * that coordinated structures are first-conjunct governed.
   */
  List<Expansion> getExpansions(PosTaggedToken posTaggedToken, ParseConfiguration parseConfiguration, int depth,
                                Map<PosTaggedToken, List<Expansion>> expansionsPerNoun) {
    List<Expansion> expansions;

    List<Expansion> myExpansions = expansionsPerNoun.get(posTaggedToken);
    if (myExpansions == null) {
      myExpansions = new ArrayList<>();

      List<PosTaggedToken> dependents = parseConfiguration.getDependents(posTaggedToken);

      DependencyNode kernel = parseConfiguration.getDetachedDependencyNode(posTaggedToken);

      // only add the kernel on its own if it meets certain criteria
      String posTagCode = posTaggedToken.getTag().getCode();
      int numDependents = dependents.size();
      if (!nonStandaloneTags.contains(posTagCode) && !(numDependents > 0 && nonStandaloneIfHasDependents.contains(posTagCode))) {
        myExpansions.add(new Expansion(kernel, sessionId));
      }

      // add the various dependents one at a time, until we hit a
      // dependent that shouldn't be included
      List<PosTaggedToken> leftHandDependents = new ArrayList<PosTaggedToken>();
      List<PosTaggedToken> rightHandDependents = new ArrayList<PosTaggedToken>();
      List<List<Expansion>> leftHandExpansionList = new ArrayList<List<Expansion>>();
      List<List<Expansion>> rightHandExpansionList = new ArrayList<List<Expansion>>();

      for (PosTaggedToken dependent : dependents) {
        // stop when we hit conjugated verbs or pronouns
        // current assumption is these will always be "to the right" of
        // the term candidate
        if (termStopTags.contains(posTagCode)) {
          break;
        }

        // recursively get the expansions for each dependent, and store
        // them either to the left or to the right
        List<Expansion> dependentExpansions = this.getExpansions(dependent, parseConfiguration, depth + 1, expansionsPerNoun);

        if (dependentExpansions.size() > 0) {
          if (dependent.getIndex() < posTaggedToken.getIndex()) {
            leftHandDependents.add(0, dependent);
            leftHandExpansionList.add(0, dependentExpansions);
          } else {
            rightHandDependents.add(dependent);
            rightHandExpansionList.add(dependentExpansions);
          }
        }
      }

      // add expansions from left and right side individually
      for (int i = 0; i < 2; i++) {
        List<List<Expansion>> oneSideExpansionList = leftHandExpansionList;
        if (i == 1)
          oneSideExpansionList = rightHandExpansionList;
        DependencyNode currentNode = kernel.cloneNode();
        DependencyNode biggestNode = null;
        for (List<Expansion> dependentExpansions : oneSideExpansionList) {
          for (Expansion dependentExpansion : dependentExpansions) {
            DependencyNode dependentNode = dependentExpansion.getNode();
            DependencyNode newNode = currentNode.cloneNode();
            newNode.addDependent(dependentNode);

            if (newNode.isContiguous()) {
              int perceivedDepth = newNode.getPerceivedDepth(this.zeroDepthLabels);

              if (perceivedDepth <= this.getMaxDepth()) {
                Expansion expansion = new Expansion(newNode, sessionId);
                myExpansions.add(expansion);
              }

              biggestNode = newNode;
            }
          }
          if (biggestNode == null)
            break;

          currentNode = biggestNode;
        }
      }

      // add dependents from both sides in combination
      if (leftHandExpansionList.size() > 0 && rightHandExpansionList.size() > 0) {
        // have both right and left-hand expansions
        DependencyNode currentLeftNode = kernel.cloneNode();
        DependencyNode biggestLeftNode = null;
        for (List<Expansion> leftExpansions : leftHandExpansionList) {
          for (Expansion leftExpansion : leftExpansions) {
            DependencyNode leftNode = leftExpansion.getNode();
            DependencyNode currentNode = currentLeftNode.cloneNode();
            currentNode.addDependent(leftNode);
            DependencyNode biggestRightNode = null;
            for (List<Expansion> rightExpansions : rightHandExpansionList) {
              for (Expansion rightExpansion : rightExpansions) {
                DependencyNode rightNode = rightExpansion.getNode();
                DependencyNode newNode = currentNode.cloneNode();
                newNode.addDependent(rightNode);

                if (newNode.isContiguous()) {
                  int perceivedDepth = newNode.getPerceivedDepth(zeroDepthLabels);
                  if (perceivedDepth <= this.getMaxDepth()) {
                    Expansion expansion = new Expansion(newNode, sessionId);
                    myExpansions.add(expansion);
                  }
                  biggestRightNode = rightExpansion.getNode();
                }
              }

              if (biggestRightNode == null)
                break;
              currentNode = currentNode.cloneNode();
              currentNode.addDependent(biggestRightNode);
            }
            biggestLeftNode = leftExpansion.getNode();
          }
          currentLeftNode = currentLeftNode.cloneNode();
          currentLeftNode.addDependent(biggestLeftNode);
        }
      }

      expansionsPerNoun.put(posTaggedToken, myExpansions);
    } // expansions have not yet been calculated for this token

    if (depth == 0) {
      // if it's top-level, we don't want the coordinating structure, nor
      // the determinant
      expansions = new ArrayList<Expansion>();
      for (Expansion expansion : myExpansions) {
        boolean include = true;
        for (DependencyNode child : expansion.getNode().getDependents()) {
          if (this.nonTopLevelLabels.contains(child.getLabel())) {
            include = false;
            break;
          }
        }
        if (include)
          expansions.add(expansion);
      }
    } else {
      expansions = myExpansions;
    }

    return expansions;
  }

  /**
   * The maximum depth for term extraction, where including a dependent's
   * dependents gives a depth of 2. Some dependents are considered
   * "zero-depth" and don't add to the depth - such as conjuncts or
   * determinants.
   */
  public int getMaxDepth() {
    return maxDepth;
  }
}
