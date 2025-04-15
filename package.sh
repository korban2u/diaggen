#!/bin/bash
echo "Building DiagGen..."
mvn clean package

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Creating macOS package..."
    jpackage --input target/ --dest target/dist --name DiagGen --main-jar diaggen-1.0-SNAPSHOT.jar --main-class com.diaggen.Main --type dmg --mac-package-name DiagGen --icon src/main/resources/images/diagram-icon.icns --app-version 1.0.0 --vendor "Ryan Korban" --copyright "Copyright 2025" --description "Générateur de diagrammes de classe UML"
else
    echo "Creating Linux package..."
    jpackage --input target/ --dest target/dist --name DiagGen --main-jar diaggen-1.0-SNAPSHOT.jar --main-class com.diaggen.Main --type deb --linux-menu-group Development --linux-shortcut --icon src/main/resources/images/diagram-icon.png --app-version 1.0.0 --vendor "Ryan Korban" --copyright "Copyright 2025" --description "Générateur de diagrammes de classe UML"
fi

echo "Package created in target/dist"