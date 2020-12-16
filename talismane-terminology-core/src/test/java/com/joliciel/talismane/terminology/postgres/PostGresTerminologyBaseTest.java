package com.joliciel.talismane.terminology.postgres;

import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.Term;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PostGresTerminologyBaseTest {
  Config config = ConfigFactory.load();
  String projectCode = config.getString("talismane.terminology.project-code");
  
  @Before
  public void before() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    database.clearDatabase();
  }
  
  @Test
  public void saveTerm() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    Term term = database.findTerm("blah");
    term.save();

    Term loadedTerm = database.findTerm("blah");
    assertEquals(term, loadedTerm);
    assertEquals("blah", loadedTerm.getText());
    assertEquals(0, loadedTerm.getFrequency());
  }

  @Test
  public void saveTermWithContexts() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    Term term = database.findTerm("blah");
    term.save();
    
    Context context1 = database.findContext(term, "blah.txt", 1, 10);
    context1.setTextSegment("blah di blah");
    context1.save();
    
    Context context2 = database.findContext(term, "blah.txt", 21, 30);
    context2.setTextSegment("and blah di blah");
    context2.save();

    Term loadedTerm = database.findTerm("blah");
    assertEquals("blah", loadedTerm.getText());
    assertEquals(2, loadedTerm.getFrequency());
    
    List<Context> contexts = loadedTerm.getContexts();
    assertEquals(context1, contexts.get(0));
    assertEquals(context2, contexts.get(1));
  }

  @Test
  public void ensureContextsAreNotMixed() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    Term term = database.findTerm("blah");
    term.save();

    Context context1 = database.findContext(term, "blah.txt", 1, 10);
    context1.setTextSegment("blah di blah");
    context1.save();

    Context context2 = database.findContext(term, "blah.txt", 21, 30);
    context2.setTextSegment("and blah di blah");
    context2.save();

    PostGresTerminologyBase otherDatabase = new PostGresTerminologyBase(projectCode + "2");
    Term sameTerm = otherDatabase.findTerm("blah");
    sameTerm.save();

    Context otherContext = otherDatabase.findContext(sameTerm, "blah.txt", 1, 10);
    otherContext.setTextSegment("blah di blah");
    otherContext.save();

    Term loadedTerm = database.findTerm("blah");
    assertEquals("blah", loadedTerm.getText());
    assertEquals(2, loadedTerm.getFrequency());

    List<Context> contexts = loadedTerm.getContexts();
    assertEquals(context1, contexts.get(0));
    assertEquals(context2, contexts.get(1));

    Term otherLoadedTerm = otherDatabase.findTerm("blah");
    assertEquals("blah", otherLoadedTerm.getText());
    assertEquals(1, otherLoadedTerm.getFrequency());

    List<Context> otherContexts = otherLoadedTerm.getContexts();
    assertEquals(otherContext, otherContexts.get(0));
  }

  @Test
  public void saveTermWithHeads() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    Term head1 = database.findTerm("head blah 1");
    head1.save();
    Context context1 = database.findContext(head1, "blah.txt", 1, 10);
    context1.setTextSegment("blah di blah");
    context1.save();
    
    Term head2 = database.findTerm("head blah 2");
    head2.save();
    Context context2 = database.findContext(head2, "blah.txt", 21, 30);
    context2.setTextSegment("and blah di blah");
    context2.save();
    
    Term term = database.findTerm("blah");
    term.addHead(head1);
    term.addHead(head2);
    term.save();

    Context context3 = database.findContext(term, "blah.txt", 21, 30);
    context3.setTextSegment("and blah di blah");
    context3.save();

    Term loadedTerm = database.findTerm("blah");
    assertEquals("blah", loadedTerm.getText());
    assertEquals(2, loadedTerm.getHeadCount());

    Set<Term> heads = loadedTerm.getHeads();
    Set<Term> expectedHeads = new HashSet<>(Arrays.asList(head1, head2));
    assertEquals(expectedHeads, heads);
  }

  @Test
  public void saveTermWithExpansions() throws Exception {
    PostGresTerminologyBase database = new PostGresTerminologyBase(projectCode);
    Term expansion1 = database.findTerm("expansion blah 1");
    expansion1.save();
    Context context1 = database.findContext(expansion1, "blah.txt", 1, 10);
    context1.setTextSegment("blah di blah");
    context1.save();

    Term expansion2 = database.findTerm("expansion blah 2");
    expansion2.save();
    Context context2 = database.findContext(expansion2, "blah.txt", 21, 30);
    context2.setTextSegment("and blah di blah");
    context2.save();

    Term term = database.findTerm("blah");
    term.addExpansion(expansion1);
    term.addExpansion(expansion2);
    term.save();

    Context context3 = database.findContext(term, "blah.txt", 21, 30);
    context3.setTextSegment("and blah di blah");
    context3.save();

    Term loadedTerm = database.findTerm("blah");
    assertEquals("blah", loadedTerm.getText());
    assertEquals(2, loadedTerm.getExpansionCount());

    Set<Term> expansions = loadedTerm.getExpansions();
    Set<Term> expectedExpansions = new HashSet<>(Arrays.asList(expansion1, expansion2));
    assertEquals(expectedExpansions, expansions);
  }
  
}
