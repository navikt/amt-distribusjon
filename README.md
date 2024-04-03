# amt-distribusjon

## Utvikling
**Lint fix:** 
```
./gradlew ktlintFormat build
```
**Build:**
```
./gradlew build
```

### TestContainers
Testene starter opp noen docker-containerer via TestContainers, dette kan ta forholdsvis lang tid og er plagsomt hvis man må gjøre dette hele tiden. Man kan konfigurere TestContainers til å ikke stoppe containerner etter testene er kjørt, noe som gjør at oppstartstiden for testene reduseres med ca 90% etter de har først startet. 

For å skru dette på må det opprettes en `.testcontainsers.properties` configfil i `$HOME`:
```shell 
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
```

En miljøvariabel `TESTCONTAINERS_REUSE=true` må også settes settes.

#### Obs
Det er viktig å være obs på at da blir disse containerene kjørende på maskinen til de blir stoppet manuelt med `docker stop {id}` eller `docker stop $(docker -ps -q)` for å stoppe alle kjørende containerer. h

Hvis man bruker TestContainers i andre prosjekter også, så kan det være en risiko for at de vil gjenbruke de samme containerne som allerede er startet av et annet prosjekt. Ved å sette et unikt label for hver container skal det teoretisk sett ikke skje: `container.withLabel("reuse.UUID", "dc04f4eb-01b6-4e32-b878-f0663d583a52")`.

Reuse kan naturligvis ikke benyttes i CI/CD.