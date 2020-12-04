package com.joliciel.talismane.terminology;

import com.joliciel.talismane.TalismaneMain;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.util.*;

public class TermExtractorMain {
  public static void main(String[] args) throws Exception {
    TalismaneMain talismaneMain = new TalismaneMain();
    
    OptionParser optionParser = talismaneMain.getOptionParser();
    OptionSpec<String> projectCodeOption = optionParser.accepts("projectCode", "terminology project code").withRequiredArg().ofType(String.class);

    optionParser.allowsUnrecognizedOptions();
    
    OptionSet options = optionParser.parse(args);
    if (args.length == 0 || options.has("help")) {
      optionParser.printHelpOn(System.out);
      return;
    }

    String sessionId = options.valueOf(talismaneMain.getSessionIdOption());
    String projectCode = options.valueOf(projectCodeOption);

    Map<String, Object> values = new HashMap<>();
    values.put("talismane.terminology.project-code", projectCode);
    values.put("talismane.terminology.language", sessionId);
    
    values.forEach((k, v) -> System.setProperty(k, v.toString()));
    
    List<String> argsList = new ArrayList<>(Arrays.asList(args));
    if (!argsList.stream().anyMatch(s -> s.startsWith("--process"))) {
      argsList.add("--process");
    }
    if (!argsList.stream().anyMatch(s -> s.startsWith("--module"))) {
      argsList.add("--module=parser");
    }
    
    talismaneMain.execute(argsList.toArray(new String[0]));
  }
}
