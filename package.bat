@echo off
echo Building DiagGen for Windows...
call mvn clean package
call jpackage --input target/ --dest target/dist --name DiagGen --main-jar diaggen-1.0-SNAPSHOT.jar --main-class com.diaggen.Main --type msi --win-dir-chooser --win-menu --win-shortcut --icon src/main/resources/images/diagram-icon.ico --app-version 1.0.0 --vendor "Ryan Korban" --copyright "Copyright 2025" --description "Générateur de diagrammes de classe UML"
echo Package created in target/dist