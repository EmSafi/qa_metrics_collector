# qa_metrics_collector
Кастомный проект, работающий с Jira API для сбора метрик тестирования и трудозатрат. 
Возможножен запуск через CI/CD с помощью команды: 

```
clean -DcurrentVersion=${CURRENT_VERSION} "-DpreviousVersionsList=${PAST_VERSIONS}" "-DmetricsList=${METRIC_LIST}" -DcollectWorklog=${COLLECT_WORKLOG} compile exec:java
```
Где: 
- CURRENT_VERSION -> текущая версия релиза
- PAST_VERSIONS -> прошлые версии релизов (через запятую)
- METRIC_LIST -> список метрик по которым нужно сформировать отчет
- COLLECT_WORKLOG -> собирать ли трудозатраты (true/false) 
 
## Пример отчета: 
[Просмотреть отчёт](https://htmlpreview.github.io/?https://raw.githubusercontent.com/EmSafi/qa_metrics_collector/main/report.html)
