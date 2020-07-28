module unearth.jdbc {
    exports unearth.jdbc;

    requires unearth.core;
    requires unearth.munch;
    requires unearth.util;
    
    requires org.flywaydb.core;
    requires org.slf4j;
    
    requires java.sql;
}
