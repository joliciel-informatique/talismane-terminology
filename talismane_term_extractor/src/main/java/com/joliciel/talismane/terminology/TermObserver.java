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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;

public interface TermObserver {
  void onNewContext(String context);
  
  void onNewTerm(Term term);

  /**
   * Collect the term observers specified in the configuration key
   * talismane.terminology.term-observers.<br/>
   * 
   * @return
   * @throws TalismaneException
   *           if a processor does not implement the TermObserver interface.
   */
    static List<TermObserver> getObservers() throws ReflectiveOperationException {
    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.terminology");

    List<TermObserver> observers = new ArrayList<>();
    List<String> classes = parserConfig.getStringList("term-observers");

    for (String className : classes) {
      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!TermObserver.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + TermObserver.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends TermObserver> clazz = untypedClass;
      
      TermObserver observer = clazz.getDeclaredConstructor().newInstance();
      observers.add(observer);
    }

    return observers;
  }
}
