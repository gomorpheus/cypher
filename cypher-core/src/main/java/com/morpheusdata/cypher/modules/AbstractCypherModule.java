package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.CypherModule;


/**
 * Created by Chris Taylor on 10/26/22.
 */
public abstract class AbstractCypherModule implements CypherModule {
  public Boolean readFromDatastore() {
    return true;
  }
}
