package com.joliciel.talismane.terminology.extractor;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.joliciel.talismane.terminology.Term;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;

public class TermAnalysisWriter implements TermObserver {
  private static final Logger LOG = LoggerFactory.getLogger(TermAnalysisWriter.class);
  private final Writer writer;
  
  public TermAnalysisWriter() throws IOException {
    Config config = ConfigFactory.load().getConfig("talismane.terminology.term-analysis-writer");
    String outFilePath = config.getString("out-file");
    File outFile = new File(outFilePath);
    File parent = outFile.getParentFile();
    if (parent != null) parent.mkdirs();
    outFile.delete();
    outFile.createNewFile();

    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    
    this.writer = writer;
  }

  @Override
  public void onNewContext(String context) {
    try {
      writer.write("\n#### Sentence: " + context + "\n");
      writer.flush();
    } catch (IOException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onNewTerm(Term term) {
    try {
      writer.write(term.getText() + "\n");
      writer.flush();
    } catch (IOException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    }
  }

}
