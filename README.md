# **qa_metrics_collector**

Кастомная программа, осуществляющая сбор метрик с помощью Jira API. 
Запуск программы осуществляется командой maven = "clean -DcurrentVersion=${CURRENT_VERSION} "-DpreviousVersionsList=${PAST_VERSIONS}" "-DmetricsList=${METRIC_LIST}" -DcollectWorklog=${COLLECT_WORKLOG} compile exec:java"
Где 
  CURRENT_VERSION = номер текущего релиза
  PAST_VERSIONS = предыдущие версия релизов, перечисленные через запятую 
  METRIC_LIST = имена метрик, которые необходимо собрать (если не указанно - собираем все метрики) 
  COLLECT_WORKLOG = true/false - нужно ли собирать трудозатраты текущего релиза 

--
Результатом выполненя программы является html файл с отчетом. 

**Чтобы посмотреть пример отчета:**
Скачать файл в директории проекта: data/report.html -> открыть его в браузере 
--
