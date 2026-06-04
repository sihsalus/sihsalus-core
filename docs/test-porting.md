# Estado del porteo de tests legacy

Contexto: el commit original `7a3c65f6` agregó ~1661 archivos de test traídos
de los omods originales de OpenMRS. La estrategia del proyecto (monolito modular
estático) es **conservar solo tests unitarios / Mockito** y **descartar los
context-sensitive** (los que arrancan el `ApplicationContext` de OpenMRS), porque
el framework de test de OpenMRS (`openmrs-api`/`openmrs-web`/`openmrs-test`) se
quitó del POM padre.

Este documento registra qué se recuperó y, sobre todo, **por qué ciertos tests
NO son porteables** — para que no se intenten re-agregar sin antes migrar el
código de producción del que dependen.

## Qué es "no migrado al monolito"

Varios módulos eran originalmente `api` + `omod` (capa web). Al pasar a monolito
se portó la capa `api` pero **NO la capa web/FHIR** de algunos módulos. Por eso
hay tests cuyo *sujeto de prueba* (una clase de `src/main`) **no existe** en el
monolito. Esos tests no compilan y no son "porteables" como unit tests: primero
habría que migrar la clase de producción.

Cómo verificarlo (ejemplo): un test importa la clase bajo prueba; si
`find modules/<m>/src/main -name '<Clase>.java'` no devuelve nada, la clase no
fue migrada.

## Recuperado (verde, en la rama)

| Módulo | Tests | Nota |
|--------|-------|------|
| `fhir2` `api/translators/impl` | 73 | Mockito puro. Deps añadidas: `hamcrest-date`, `JUnitParams`, `mockito-core`. Recuperado `FhirTestConstants` + dataset `fhir-ucum-common.csv`. |
| `fhir2` `api/impl` | 24 | + helpers `MockIBundleProvider` (r3/r4) y dep `co.unruly:java-8-matchers`. |
| `queue` | 10 | api + web resources/controller (`javax.servlet`→`jakarta.servlet`). |
| `billing` | 2 | `DrugOrderBillingStrategyTest`, `TestOrderBillingStrategyTest` (estrategias de cobro, sin FHIR). |

Ajustes comunes al recuperar: `javax.*`→`jakarta.*`; API vieja de Mockito
(`org.mockito.Matchers`→`ArgumentMatchers`, `org.mockito.runners`→`org.mockito.junit`,
`anyListOf(X.class)`→`anyList()`); fechas con `Locale.ENGLISH`;
`commons-lang`→`commons-lang3`; `spotless:apply`. Son JUnit4 y corren bajo
`junit-vintage-engine`.

Dos métodos de `fhir2` quedan `@Ignore`: con HAPI FHIR 6.x,
`AllergyIntolerance.hasCategory()` / `hasSeverity()` devuelven `false` para el
enum `NULL`, así que el translator (correctamente guardado) ya no setea el valor;
las aserciones asumían el comportamiento previo al upgrade.

## NO porteable (descartado, con razón verificada)

| Módulo / tests | Por qué |
|----------------|---------|
| `fhir2` `providers/r3` + `r4` (~69) | Extienden `BaseFhir*ResourceProviderWebTest`, que requiere `org.openmrs.module.fhir2.web.servlet.FhirRestServlet` — **capa web del omod NO migrada** (11 clases web `javax`). |
| `fhir2` `narrative` (1) | Requiere Thymeleaf + capa web. |
| `billing` FHIR (3): `FhirInvoiceServiceImplTest`, `InvoiceFhirResourceProviderTest`, `InvoiceTranslatorImplTest` | Las clases bajo prueba **no existen en `main`**: `FhirInvoiceService(Impl)`, `FhirInvoiceDao`, `InvoiceTranslator(Impl)`, `InvoiceFhirResourceProvider`. Capa FHIR de billing no migrada. |
| `patientflags` FHIR (5): `FhirFlagServiceImplTest`, `FlagFhirResourceProviderTest`, `FlagTranslatorImplTest`, `PatientFlagTranslatorImplTest`, `TagTranslatorImplTest` | Igual: `FhirFlagService(Impl)`, `FlagFhirResourceProvider`, `FlagTranslatorImpl`, `PatientFlagTranslatorImpl`, `TagTranslatorImpl` **no existen en `main`**. |
| `bedmanagement` (1): `atomfeed/BedTagMapAdviceTest` | Depende de libs externas `org.ict4h.atomfeed.*` (no en el classpath). |
| `legacyui` (1): `web/extension/ExtensionUtilTest` | Depende de API legacy `org.openmrs.module.web.extension.*` (openmrs-web). |
| `oauth2login` (1): `OAuth2UserInfoAuthenticationSchemeTest` | Llama `authScheme.setDaemonToken(...)`, método removido en el monolito estático (sin daemon tokens). |

Para recuperar cualquiera de estos primero hay que **migrar la clase de
producción correspondiente** (p.ej. la capa web FHIR de billing/patientflags o
`FhirRestServlet`), lo cual está fuera del alcance del porteo de tests.

## Diferido

`appointments` (4 tests, fuera de 1 que usa PowerMock y se descarta): se migra
la API vieja de Mockito y compilan, pero **3 métodos fallan por diferencias de
comportamiento** (aserciones tipo `expected:<0> but was:<3>` en
`AppointmentServiceUnavailabilityConflictTest` y un "Should have generated
appointment number" en `AppointmentRecurringPatternServiceImplTest`). Quedan sin
commitear hasta investigar si es un cambio de comportamiento intencional del
`main` (en cuyo caso `@Ignore` con razón) o un bug.

## Cómo retomar

1. Recuperar del commit original: `git checkout 7a3c65f6 -- <ruta>`.
2. Filtrar context-sensitive (`extends BaseModule*ContextSensitiveTest` /
   `BaseFhir*ResourceProviderWebTest` / `*IntegrationTest`) y los que referencien
   clases de `main` no migradas (verificar con `find`).
3. Aplicar los ajustes comunes de arriba + `mvn spotless:apply -pl <módulo>`.
4. `mvn test -pl <módulo>` y revisar diferencias de comportamiento.
