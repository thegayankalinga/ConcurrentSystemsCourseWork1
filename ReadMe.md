# Jar Creation Steps

`cd` to the solution folder

## Compile the source code
```bash
javac -d out src/**/*.java
```

## Create the Jar File
```bash
jar cfm TicketSystemCw1Try004.jar manifest.txt -C out .
```
### Different Named File
```bash
jar cfm TicketSimulator.jar manifest.txt -C out .
```

## Run the JAR
```bash
java -jar TicketSimulator.jar
```