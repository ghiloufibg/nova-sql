@echo off
echo Starting Nova SQL JavaFX UI...
cd nova-sql-ui
java --module-path "C:\Program Files\JavaFX\javafx-sdk-21.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp "target\nova-sql-ui-1.0.0.jar;target\lib\*;..\nova-sql-core\target\nova-sql-core-1.0.0.jar" com.novasql.ui.NovaFXApplication
pause