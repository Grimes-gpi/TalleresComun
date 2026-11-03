# Aplicación Android

Esta carpeta es el proyecto que se abre en Android Studio. No contiene el backend ni una base de datos.

La app consume la API REST del proyecto independiente `backend/`. La dirección inicial en un emulador es `http://10.0.2.2:8081/`; para un teléfono físico, configura en el botón **Conexión** la IP local del computador y el puerto `8081`.

La app no contiene usuario ni contraseña de PostgreSQL. Todas las lecturas y escrituras pasan por la API.
