module unearth.analysis {
    exports unearth.analysis;
    
    requires unearth.core;
    requires unearth.munch;
    
    requires org.slf4j;
    requires com.datastax.oss.driver.core;
    requires com.datastax.oss.driver.querybuilder;
    requires com.datastax.oss.protocol;
    requires unearth.util;
}
