# Set JAVA_HOME to JDK 22
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"

# Add Java 22 and Maven to PATH
$env:PATH = "C:\Program Files\Java\jdk-22\bin;C:\Users\Admin\scoop\apps\maven\current\bin;$env:PATH"

# Run tests but exclude controller tests
mvn test -Dtest="!*ControllerTest"