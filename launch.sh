#!/bin/sh
java -p dist:dist/lib -cp "dist/lib/*" -Xmx12G -m es.uvigo.esei.sing.vacbot.main
