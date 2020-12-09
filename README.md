# Talismane Term Extractor
Terminology extraction with Talismane

## Setup

Install docker-compose for your operating system.

```
make start-dep
make create-schema
```

## Building the project
```
mvn clean install
```

## Term extraction
```
java -Dconfig.file=terms-fr.conf -jar talismane_term_extractor/target/talismane-term-extractor-[VERSION]-shaded.jar --sessionId=fr --process --inFile="test.tal" --module=parser
```

Where `terms-fr.conf` looks like:
```
include "/application"

talismane {
  terminology {
    project-code = "test"
    language = fr
  }
}
```

## Term viewing
```
java -Dconfig.file=terms-fr.conf -jar talismane_term_viewer/target/talismane-term-viewer-[VERSION]-shaded.jar 
```

