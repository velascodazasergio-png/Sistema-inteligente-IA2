# Informe de análisis de datos

**Fuente:** base de datos del sistema cargada con [`database/data.sql`](../../database/data.sql): 150 incidentes sintéticos entre el 14 de septiembre y el 12 de noviembre de 2026, 10 usuarios, 6 categorías, 10 artículos de conocimiento y 5 consultas a la IA.

**Consultas:** todas están en [`consultas.sql`](consultas.sql) (Q1 a Q12). Se ejecutaron sobre H2 en modo PostgreSQL con los datos iniciales; los resultados de abajo son la salida literal.

**Reglas de cálculo:** iguales a las del backend (`AnalyticsService`).
- Un incidente está **resuelto** si su estado es `RESOLVED` o `CLOSED`.
- El **tiempo de atención** es `updated_at − created_at` de los incidentes resueltos.

> Los datos son sintéticos. Las conclusiones describen este conjunto de datos y sirven para validar el sistema; con datos reales deben recalcularse con las mismas consultas.

---

## 1. Indicadores principales (Q1)

| Indicador | Valor |
|---|---|
| Total de incidentes | 150 |
| Resueltos o cerrados | 130 |
| Pendientes | 20 |
| Tasa de resolución | **86,67 %** |
| Tiempo promedio de atención | **4,74 h** |

Estos valores coinciden con los que muestra el Dashboard (`GET /api/v1/analytics/dashboard`): 86,67 % y 4,74 h. Así se cumple la regla del enunciado de que el dashboard refleje las consultas reales.

## 2. Distribución de los incidentes

### Por categoría (Q2)

| Categoría | Incidentes | % del total | % resueltos | Horas promedio |
|---|---:|---:|---:|---:|
| Identity & Access Management | 38 | 25,33 | 92,11 | 4,27 |
| Networking & Connectivity | 34 | 22,67 | 85,29 | 4,78 |
| Database & Storage | 27 | 18,00 | 85,19 | **5,35** |
| Cloud Infrastructure & DevOps | 26 | 17,33 | 84,62 | 4,82 |
| Endpoint & Hardware | 13 | 8,67 | 84,62 | 4,32 |
| Enterprise Applications & ERP | 12 | 8,00 | 83,33 | 5,20 |

### Por prioridad (Q3)

| Prioridad | Incidentes | Pendientes | Horas promedio |
|---|---:|---:|---:|
| Crítica | 38 | 6 | **5,13** |
| Alta | 51 | 7 | 4,78 |
| Media | 36 | 4 | 4,61 |
| Baja | 25 | 3 | 4,30 |

### Por estado (Q4)

| Estado | Incidentes |
|---|---:|
| Resuelto | 66 |
| Cerrado | 64 |
| En análisis IA | 8 |
| Asignado a técnico | 7 |
| Abierto | 5 |

## 3. Tendencias

### Mensual (Q5)

| Mes | Registrados | Resueltos | % resueltos |
|---|---:|---:|---:|
| Septiembre 2026 | 46 | 46 | 100 % |
| Octubre 2026 | 83 | 73 | 88 % |
| Noviembre 2026 (al día 12) | 21 | 11 | 52 % |

### Semanal (Q6)

| Semana ISO | 38 | 39 | 40 | 41 | 42 | 43 | 44 | 45 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Registrados | 19 | 19 | 19 | 18 | 19 | 19 | 19 | 18 |
| Resueltos | 19 | 19 | 19 | 18 | 18 | 15 | 12 | 10 |
| **Pendientes** | 0 | 0 | 0 | 0 | **1** | **4** | **7** | **8** |

## 4. Tiempos de resolución (Q7)

| Rango | Incidentes | % de los resueltos |
|---|---:|---:|
| 1 a 4 h | 37 | 28,5 % |
| 4 a 8 h | 73 | **56,2 %** |
| 8 a 24 h | 20 | 15,4 % |

Ningún incidente se resolvió en menos de 1 hora ni tardó más de 24 horas.

## 5. Recurrencia de problemas (Q12)

| Problema | Ocurrencias |
|---|---:|
| High disk I/O latency on reporting replica DB-02 | 14 |
| Kubernetes payment-service pods OOMKilled (Exit 137) | 14 |
| Active Directory account locked out repeatedly | 13 |
| BitLocker recovery key prompted after UEFI BIOS update | 13 |
| Okta SSO SAML assertion rejected by CRM portal | 13 |
| PostgreSQL HikariPool-1 connection timeout on ERP node | 13 |
| Corporate VPN tunnel drops with error 809 | 12 |
| ERP invoice PDF generation stuck in pending queue | 12 |
| MFA hardware token out of sync for finance user | 12 |
| NGINX Ingress returning 502 Bad Gateway on checkout API | 12 |
| **Total de los 10 problemas** | **128 de 150 (85,3 %)** |

## 6. Base de conocimiento y asistente IA (Q8 a Q11)

| Indicador | Valor |
|---|---|
| Artículos activos y verificados (utilizables por la IA) | 10 |
| Incidentes por artículo utilizable | de 12,0 (ERP) a **19,0** (Identity & Access Management) |
| Consultas a la IA | 5: 4 respondidas, 1 con error de conexión |
| Confianza promedio de las respuestas | 0,925 |
| Similitud promedio de las fuentes citadas | 0,915 (mín. 0,88, máx. 0,95) |
| Calificación promedio | **4,75 / 5** (4 calificaciones, 100 % "útil") |

---

## 7. Interpretación

1. **El servicio resuelve bien, pero ya no al ritmo de la entrada.** Cada semana entran entre 18 y 19 incidentes. Hasta la semana 41 se resolvían todos; desde la 42 empiezan a quedar pendientes, y las semanas 44 y 45 concentran 15 de los 20 abiertos. El indicador global de 86,67 % oculta esa tendencia, porque se apoya en los meses anteriores.
2. **La prioridad no acelera la atención.** Los incidentes críticos tardan más (5,13 h) que los de prioridad baja (4,30 h), y concentran 6 de los 20 pendientes. Puede deberse a que los críticos son más complejos, pero también indica que la prioridad no está cambiando el orden de atención.
3. **La mayor parte del trabajo es repetitivo.** Diez problemas explican el 85,3 % de los incidentes, y cada uno se repite entre 12 y 14 veces. Es el escenario donde una base de conocimiento con búsqueda semántica aporta más valor.
4. **Database & Storage es la categoría más lenta** (5,35 h, 0,6 h por encima del promedio). Identity & Access Management es la de mayor volumen, pero también la más eficiente (92,11 % resuelta, 4,27 h).
5. **El asistente IA muestra buenos resultados iniciales, pero con una muestra muy pequeña.** Las 4 respuestas calificadas promedian 4,75 estrellas y citan fuentes con similitud de 0,88 o más. Cuatro casos no permiten generalizar.

## 8. Conclusiones respaldadas por la evidencia

1. **Hay un retraso acumulado reciente que el indicador global no muestra.**
   - *Evidencia:* Q6. Los pendientes por semana pasan de 0 (semanas 38 a 41) a 1, 4, 7 y 8 (semanas 42 a 45). En noviembre solo está resuelto el 52 % de lo registrado (Q5), frente al 100 % de septiembre.
   - *Recomendación:* además de la tasa global, seguir la tasa de resolución por semana y la antigüedad de los pendientes. El Dashboard ya permite filtrar por los últimos 7 o 30 días y lo compara con el periodo anterior.

2. **La prioridad crítica no se traduce en una atención más rápida.**
   - *Evidencia:* Q3. Los críticos promedian 5,13 h frente a 4,30 h de los de prioridad baja, y tienen la mayor cantidad de pendientes junto con los de prioridad alta (6 y 7).
   - *Recomendación:* definir un acuerdo de nivel de servicio (SLA) por prioridad y ordenar la cola de trabajo por prioridad, no por llegada.

3. **Ampliar la base de conocimiento es la acción con mayor retorno.**
   - *Evidencia:* Q12 y Q11. El 85,3 % de los incidentes corresponde a 10 problemas recurrentes, pero solo hay 10 artículos utilizables, uno o dos por categoría. Identity & Access Management atiende 19 incidentes por artículo.
   - *Recomendación:* documentar y verificar primero un artículo por cada problema recurrente, empezando por las categorías con más incidentes por artículo. Así el asistente RAG podrá responder a la mayoría de los casos con fuentes verificadas.

4. **El asistente IA es prometedor, pero necesita más evaluaciones antes de medir su impacto.**
   - *Evidencia:* Q8 a Q10. Calificación promedio de 4,75, 100 % de respuestas "útiles" y similitud promedio de 0,915, pero sobre 4 respuestas. Además, 1 de las 5 consultas terminó en error de conexión.
   - *Recomendación:* pedir la calificación de cada respuesta (el Historial ya lo permite) y vigilar la proporción de consultas en error o sin resultados.

## 9. Cómo reproducir el análisis

```bash
# Backend con una base H2 en archivo, para poder consultarla mientras corre
cd backend
DB_URL="jdbc:h2:file:./analysisdb;AUTO_SERVER=TRUE;MODE=PostgreSQL" mvn spring-boot:run

# En otra terminal: ejecutar una consulta de consultas.sql
java -cp ~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./backend/analysisdb;AUTO_SERVER=TRUE;MODE=PostgreSQL" -user sa \
  -sql "SELECT status, COUNT(*) FROM incidents GROUP BY status"
```

Con PostgreSQL (Docker Compose), las mismas consultas se ejecutan con `psql`.
