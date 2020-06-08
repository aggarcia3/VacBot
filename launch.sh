#!/bin/sh
java -p dist:dist/lib -cp "dist/lib/*" -Xmx12G --add-opens java.base/java.lang=com.google.guice -m es.uvigo.esei.sing.vacbot.main $@
