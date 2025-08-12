@echo off
setlocal

set ES_HOST=localhost:9200
set ES_USER=elastic
set ES_PASS=s0Fov=RW4zmt4s_Pntd7
set INDEX_NAME=xreal_tech_faq

echo Creating Elasticsearch index: %INDEX_NAME%

REM Create index with mappings
curl -u %ES_USER%:%ES_PASS% -X PUT "http://%ES_HOST%/%INDEX_NAME%" ^
  -H "Content-Type: application/json" ^
  -d @elasticsearch-mapping.json

echo.
echo Elasticsearch index initialization completed!
pause