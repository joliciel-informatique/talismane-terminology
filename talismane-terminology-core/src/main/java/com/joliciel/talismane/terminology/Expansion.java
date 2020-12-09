package com.joliciel.talismane.terminology;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.DependencyNode;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.filters.UppercaseSeriesFilter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.*;

public class Expansion {
  private final DependencyNode node;
  private final PosTaggerLexicon lexicon;
  private final String sessionId;
  
  private List<Expansion> children = null;
  private List<Expansion> parents = null;
  private Set<PosTaggedToken> tokenSet = null;
  private String text = null;
  
  private final Set<String> zeroDepthLabels;
  private final Set<String> termStopTags;
  private final Set<String> nonStandaloneTags;
  private final Set<String> nonStandaloneIfHasDependents;
  private final Set<String> prepositionalTags;
  private final Set<String> nominalTags;
  private final Set<String> adjectivalTags;
  private final Set<String> determinentTags;
  private final Set<String> coordinationLabels;
  private final String lemmaNumber;
  private final String lemmaGender;

  public Expansion(DependencyNode node, String sessionId) {
    this.sessionId = sessionId;
    
    Config config = ConfigFactory.load().getConfig("talismane.terminology");

    String language = config.getString("language");
    Config languageConfig = config.getConfig(language);

    this.adjectivalTags = new HashSet<>(languageConfig.getStringList("adjectival-tags"));
    this.coordinationLabels = new HashSet<>(languageConfig.getStringList("coordination-labels"));
    this.determinentTags = new HashSet<>(languageConfig.getStringList("determinent-tags"));
    this.lemmaGender = languageConfig.getString("lemma-gender");
    this.lemmaNumber = languageConfig.getString("lemma-number");
    this.nominalTags = new HashSet<>(languageConfig.getStringList("nominal-tags"));
    this.nonStandaloneIfHasDependents = new HashSet<>(languageConfig.getStringList("non-standalone-if-has-dependents"));
    this.nonStandaloneTags = new HashSet<>(languageConfig.getStringList("non-standalone-tags"));
    this.prepositionalTags = new HashSet<>(languageConfig.getStringList("prepositional-tags"));
    this.termStopTags = new HashSet<>(languageConfig.getStringList("term-stop-tags"));
    this.zeroDepthLabels = new HashSet<>(languageConfig.getStringList("zero-depth-labels"));
    
    this.node = node;
    this.lexicon = TalismaneSession.get(sessionId).getMergedLexicon();
  }

  public List<Expansion> getChildren() {
    if (this.children == null) {
      children = new ArrayList<>();
      for (DependencyNode child : node.getDependents()) {
        String posTagCode = child.getPosTaggedToken().getTag().getCode();
        if (prepositionalTags.contains(posTagCode)) {
          if (child.getDependents().size() > 0) {
            DependencyNode realChild = child.getDependents().iterator().next().cloneNode();
            Expansion realChildExpansion = new Expansion(realChild, sessionId);
            if (realChildExpansion.display() != null && realChildExpansion.display().length() > 0)
              children.add(new Expansion(realChild, sessionId));
          }
        } else if (nominalTags.contains(posTagCode)) {
          Expansion childExpansion = new Expansion(child, sessionId);
          if (childExpansion.display() != null && childExpansion.display().length() > 0)
            children.add(new Expansion(child, sessionId));
        }
      }
    }
    return children;
  }

  public List<Expansion> getParents() {
    if (this.parents == null) {
      parents = new ArrayList<>();
      List<DependencyNode> leftDependents = new ArrayList<DependencyNode>();
      List<DependencyNode> rightDependents = new ArrayList<DependencyNode>();
      for (DependencyNode child : node.getDependents()) {
        if (child.getPosTaggedToken().getIndex() < node.getPosTaggedToken().getIndex()) {
          leftDependents.add(child);
        } else {
          rightDependents.add(child);
        }
      }
      if (leftDependents.size() > 0) {
        DependencyNode leftParent = node.cloneNode();
        leftParent.removeNode(leftDependents.get(0));
        parents.add(new Expansion(leftParent, sessionId));
      }
      if (rightDependents.size() > 0) {
        DependencyNode rightParent = node.cloneNode();
        rightParent.removeNode(rightDependents.get(rightDependents.size() - 1));
        parents.add(new Expansion(rightParent, sessionId));
      }
    }
    return parents;
  }

  public DependencyNode getNode() {
    return node;
  }

  public String display() {
    if (text == null) {
      DependencyNode startNode = node;
      String posTagCode = node.getPosTaggedToken().getTag().getCode();
      if (!(nominalTags.contains(posTagCode))) {
        return null;
      }

      Set<PosTaggedToken> tokensToDisplay = this.getTokenSet();

      // if top level, return lemma for noun, and bring modifying
      // adjectives to lemmatised form
      List<LexicalEntry> startNodeEntries = startNode.getPosTaggedToken().getLexicalEntries();
      Optional<LexicalEntry> headNounEntry = this.headOption(startNodeEntries);
      boolean lemmatiseHead = false;
      String headNounGender = lemmaGender;

      if (headNounEntry.isPresent()) {
        if (headNounEntry.get().hasAttribute(LexicalAttribute.Number) && !headNounEntry.get().getNumber().contains(lemmaNumber)) {
          lemmatiseHead = true;
          if (headNounEntry.get().hasAttribute(LexicalAttribute.Gender) && headNounEntry.get().getGender().size() == 1)
            headNounGender = headNounEntry.get().getGender().get(0);
        }
      }

      Token lastToken = null;
      String sentence = startNode.getParseConfiguration().getPosTagSequence().getTokenSequence().getSentence().getText().toString();
      StringBuilder stringBuilder = new StringBuilder();
      for (PosTaggedToken posTaggedToken : tokensToDisplay) {
        Token currentToken = posTaggedToken.getToken();
        String tokenText = currentToken.getOriginalText();
        if (Character.isUpperCase(tokenText.charAt(0))) {
          // lowercase any known words
          tokenText = UppercaseSeriesFilter.getKnownWord(sessionId, tokenText);
        }

        if (lemmatiseHead && posTaggedToken.equals(startNode.getPosTaggedToken())) {
          if (headNounEntry.isPresent()) {
            List<? extends LexicalEntry> lemmaFormEntries = lexicon.getEntriesMatchingCriteria(headNounEntry.get(), node.getPosTaggedToken().getTag(),
                null, lemmaNumber);
            LexicalEntry lemmaFormEntry = null;
            if (lemmaFormEntries.size() > 0)
              lemmaFormEntry = lemmaFormEntries.get(0);

            if (lemmaFormEntry != null)
              tokenText = lemmaFormEntry.getWord();
          }
        } else if (lemmatiseHead && adjectivalTags.contains(posTaggedToken.getTag().getCode())) {

          boolean lemmatiseModifier = false;
          if (node.getPosTaggedToken().equals(startNode.getParseConfiguration().getHead(posTaggedToken))) {
            lemmatiseModifier = true;
          } else {
            // handle coordination as well - find the parent of
            // the entire structure
            DependencyArc arc = startNode.getParseConfiguration().getGoverningDependency(posTaggedToken);
            if (arc != null) {
              PosTaggedToken parent = arc.getHead();
              while (arc != null && coordinationLabels.contains(arc.getLabel())) {
                arc = startNode.getParseConfiguration().getGoverningDependency(parent);
                parent = arc.getHead();
              }
              if (node.getPosTaggedToken().equals(parent))
                lemmatiseModifier = true;
            }
          }
          if (lemmatiseModifier) {
            Optional<LexicalEntry> pluralEntry = this.headOption(posTaggedToken.getLexicalEntries());
            if (pluralEntry.isPresent() && !pluralEntry.get().getNumber().contains(lemmaNumber)) {
              List<? extends LexicalEntry> lemmaFormEntries = lexicon.getEntriesMatchingCriteria(pluralEntry.get(), posTaggedToken.getTag(),
                  headNounGender, lemmaNumber);
              LexicalEntry lemmaFormEntry = null;
              if (lemmaFormEntries.size() > 0)
                lemmaFormEntry = lemmaFormEntries.get(0);

              if (lemmaFormEntry != null)
                tokenText = lemmaFormEntry.getWord();
            }

            if (pluralEntry == null) {
              tokenText = TalismaneSession.get(sessionId).getLinguisticRules().makeAdjectiveSingular(tokenText);
            }
          }
        } // is this some sort of plural entry that needs to be
        // singularised?

        if (lastToken == null) {
          stringBuilder.append(tokenText);
        } else if (currentToken.getIndex() - lastToken.getIndex() == 1) {
          stringBuilder.append(sentence.substring(lastToken.getEndIndex(), currentToken.getStartIndex()));
          stringBuilder.append(tokenText);
        } else {
          stringBuilder.append(" ");
          stringBuilder.append(tokenText);
        }
        lastToken = currentToken;
      }
      text = stringBuilder.toString().trim();
    }
    return text;
  }

  private <T> Optional<T> headOption(List<T> list) {
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  @Override
  public String toString() {
    return this.node.toString();
  }

  Set<PosTaggedToken> getTokenSet() {
    if (tokenSet == null) {
      tokenSet = new TreeSet<PosTaggedToken>();
      this.collectNodesForDisplay(this.node, tokenSet, 1);
    }
    return tokenSet;
  }

  void collectNodesForDisplay(DependencyNode node, Set<PosTaggedToken> tokensToDisplay, int depth) {
    if (this.shouldDisplay(node, depth))
      tokensToDisplay.add(node.getPosTaggedToken());
    for (DependencyNode child : node.getDependents()) {
      String posTagCode = child.getPosTaggedToken().getTag().getCode();
      int newDepth = depth + 1;
      if (determinentTags.contains(posTagCode))
        newDepth = depth;
      this.collectNodesForDisplay(child, tokensToDisplay, newDepth);
    }
  }

  boolean shouldDisplay(DependencyNode node, int depth) {
    String posTagCode = node.getPosTaggedToken().getTag().getCode();
    if (depth == 1 && determinentTags.contains(posTagCode)) {
      return false;
    }

    int numRealDependents = node.getParseConfiguration().getDependents(node.getPosTaggedToken()).size();
    int numAttachedDependents = node.getDependents().size();
    if (numAttachedDependents == 0 && (nonStandaloneTags.contains(posTagCode))
        || (numRealDependents > 0 && nonStandaloneIfHasDependents.contains(posTagCode))) {
      return false;
    }
    return true;
  }

  public int getLexicalWordCount() {
    int lexicalWordCount = 0;
    for (PosTaggedToken posTaggedToken : this.getTokenSet()) {
      if (posTaggedToken.getTag().getOpenClassIndicator() == PosTagOpenClassIndicator.OPEN) {
        lexicalWordCount++;
      }
    }
    return lexicalWordCount;
  }
}
