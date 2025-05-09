# ECG Monitor
# Sistema de Monitorización ECG en Tiempo Real con JavaFX

![Monitor de ECG - Pantalla Principal](https://i.imgur.com/qwVmK5X.png)

Este proyecto presenta un sistema de software para la **monitorización en tiempo real de señales electrocardiográficas (ECG)**. Desarrollado íntegramente utilizando el robusto framework **JavaFX** para la construcción de la interfaz gráfica de usuario, la aplicación está diseñada para interactuar y procesar datos de ECG provenientes de un sensor externo, específicamente el módulo **AD8232 Heart Rate Monitor**, conectado a un microcontrolador.

La arquitectura del sistema permite una integración fluida con dispositivos como el **NodeMCU** (ESP8266/ESP32), dada su popularidad y facilidad para la comunicación serial. Sin embargo, el diseño modular asegura la compatibilidad con **cualquier otro microcontrolador** (e.g., Arduino Uno, ESP32, etc.) que sea capaz de leer los datos del sensor AD8232 y transmitir los valores analógicos o procesados a través de un puerto serial estándar.

El sistema ofrece una representación gráfica dinámica de la forma de onda del ECG, realiza un cálculo preciso de la frecuencia cardíaca en pulsaciones por minuto (BPM) y proporciona retroalimentación tanto visual como auditiva ante la detección de cada latido. Adicionalmente, incluye funcionalidades para la gestión de información básica del paciente y un sistema de alerta en tiempo real ante la desconexión de los electrodos.

---

## ♥ Características Destacadas

El presente sistema de monitorización de ECG en tiempo real incorpora las siguientes funcionalidades y capacidades clave:

* **Adquisición y Visualización Dinámica de ECG:**
    * Representación gráfica continua y de alta fidelidad de la forma de onda del electrocardiograma.
    * Interfaz de usuario que permite la observación detallada de la señal en un entorno de tiempo real.
* **Análisis de Frecuencia Cardíaca (BPM):**
    * Implementación de algoritmos para la detección precisa de picos R en la señal de ECG.
    * Cálculo y visualización en tiempo real de las pulsaciones por minuto (BPM) derivadas de los intervalos RR.
* **Comunicación Serial Robusta y Flexible:**
    * Establecimiento y gestión eficiente de la comunicación con puertos seriales, utilizando la librería `jSSC`.
    * Capacidad para detectar y listar dinámicamente los puertos seriales disponibles en el sistema, facilitando la configuración por parte del usuario.
* **Gestión de Información de Pacientes:**
    * Módulo para la entrada y visualización de datos demográficos y médicos básicos del paciente (nombre, género, fecha de nacimiento, historial clínico) al inicio de la sesión de monitorización.
* **Retroalimentación de Latidos Cardíacos:**
    * Indicador visual prominente en la gráfica que señala cada latido cardíaco detectado.
    * Retroalimentación auditiva mediante un sonido configurable (un "beep") que acompaña cada detección de latido.
* **Sistema de Alerta de Desconexión de Electrodos:**
    * Detección automática de la pérdida de señal o la desconexión física de los electrodos del sensor.
    * Activación de un overlay visual intuitivo y claro (`"SE HAN DESCONECTADO LOS ELECTRODOS"`) para notificar al usuario sobre el estado de la conexión.
* **Control de Audio Integrado:**
    * Funcionalidad programática (`mute()` y `unmute()`) para silenciar o restaurar la retroalimentación auditiva del latido, permitiendo un control total sobre las alertas sonoras sin interrumpir el proceso de monitorización de la señal.

---

## 💻 Tecnologías Empleadas

El desarrollo de este sistema ha sido posible gracias a la integración de las siguientes tecnologías y librerías clave:

* **Lenguaje de Programación:** Java
* **Framework de Interfaz de Usuario (UI):** JavaFX
* **Sistema de Construcción (Build Tool):** Apache Maven (utilizado para la gestión de dependencias, compilación y empaquetado del proyecto).
* **Librerías Principales de Terceros:**
    * **jSSC (Java Simple Serial Connector):** Indispensable para la implementación de la comunicación serie bidireccional con dispositivos hardware.
    * **AudioCue:** Utilizada para la gestión eficiente de recursos de audio y la reproducción de clips de sonido cortos con baja latencia.

---

## 👨‍💻 Guía de Configuración y Ejecución

### Prerrequisitos del Sistema

* **Java Development Kit (JDK):** Versión 11 o superior (se recomienda una versión LTS como JDK 17 para mayor estabilidad y soporte a largo plazo).
* **Apache Maven:** Versión 3.6.0 o superior, correctamente instalado y configurado en la variable de entorno `PATH`.
* **Entorno de Desarrollo Integrado (IDE):** Se recomienda encarecidamente el uso de IntelliJ IDEA, Eclipse o VS Code, con soporte para proyectos Java y Maven.

### Requisitos de Hardware Específicos

* **Módulo Sensor de Ritmo Cardíaco:** Un sensor **AD8232** (o equivalente) para la adquisición de la señal analógica del ECG.
* **Microcontrolador:** Un microcontrolador como **NodeMCU** (ESP8266/ESP32), Arduino Uno, o cualquier otro capaz de:
    1.  Leer la salida analógica del sensor AD8232.
    2.  Enviar estos datos (generalmente valores enteros discretos que representan la amplitud de la señal) a través de un puerto serial USB a la computadora.
    * **Nota Importante:** Asegúrese de que el **baud rate** (tasa de baudios) configurado en el firmware del microcontrolador coincida exactamente con la tasa de baudios preestablecida en el código de la aplicación (e.g., 115200 baudios).



## 🖥️ Guía de Uso del Sistema


1.  **Inicio de la Aplicación:** Al ejecutar la aplicación, se presentará una pantalla inicial intuitiva donde el usuario puede ingresar los datos demográficos y médicos del paciente, y seleccionar el puerto serial asociado al microcontrolador.
3.  **Selección de Puerto Serial:** Elija el puerto serial correcto de la lista desplegable que corresponde a la conexión de su dispositivo de adquisición de ECG.
4.  **Inicio de la Monitorización:** Haga clic en el botón "Iniciar Monitor ECG". La aplicación transicionará a la pantalla de monitorización, donde se iniciará la representación gráfica en tiempo real de la señal del ECG.
5.  **Interpretación de Datos:** Durante la monitorización, el usuario podrá observar la forma de onda del ECG, el valor de BPM que se actualiza dinámicamente y el indicador visual de latido, proporcionando una visión clara del estado cardíaco.
6.  **Gestión de Alertas:** El sistema está diseñado para notificar al usuario mediante un overlay de "Electrodos Desconectados" si la señal del sensor se interrumpe o los electrodos pierden contacto.
7.  **Control de Audio:** La retroalimentación auditiva de los latidos puede ser activada o desactivada utilizando la funcionalidad de silenciar/restaurar, ofreciendo flexibilidad al usuario.

---

## 📸 Capturas de Pantalla


### Pantalla de Inicio (Entrada de Datos del Paciente y Selección de Puerto Serial)
![Pantalla de Ingreso de Datos](https://i.imgur.com/2gTmyAb.png)

### Pantalla del Monitor ECG en Funcionamiento (Visualización en Tiempo Real)
![Monitoreo](https://i.imgur.com/qwVmK5X.png)

### Alerta de "Electrodos Desconectados"
![Control de Estado de Electrodos]([https://i.imgur.com/qwVmK5X.png](https://i.imgur.com/RQMeE5J.png)

---

## 📂 Estructura del Proyecto

El proyecto sigue una estructura de paquetes modular y descriptiva, diseñada para promover la separación de responsabilidades y facilitar la mantenibilidad:

* `com.streelet.ecg_java_app`: Contiene las clases fundamentales de la aplicación JavaFX, incluyendo la clase principal de arranque (`App`) y los controladores de las vistas (`StartController`, `EcgMonitorController`).
* `com.streelet.ecg_java_app.model`: Dedicado a las clases que encapsulan la lógica de negocio relacionada con el procesamiento y análisis de datos de ECG, como `EcgDataModel` (para la detección de picos R y cálculo de BPM) y la interfaz `EcgPeakListener`.
* `com.streelet.ecg_java_app.serial`: Agrupa las clases responsables de la gestión de la comunicación serial, incluyendo `SerialDataManager` (para la lectura y escritura en puertos seriales) y la interfaz `SerialDataListener`. También puede contener clases para pruebas (`SerialTestReader`).
* `com.streelet.ecg_java_app.sound`: Contiene la clase `Beep`, encargada de la inicialización y control de la reproducción de audio (sonido de latido) utilizando la librería AudioCue.
* `src/main/resources/com/streelet/ecg_java_app`: Este directorio aloja los archivos de recursos de la aplicación, como los archivos FXML que definen las interfaces de usuario (`design.fxml`, `monitor.fxml`, `ElectrodesDisconnectedOverlay.fxml`), hojas de estilo CSS (`monitor.css`, `styles.css`), y los recursos de audio (`/sounds/beep.wav`).


## 📄 Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. Para obtener información detallada sobre los términos y condiciones de la licencia, por favor, consulte el archivo `LICENSE` incluido en el directorio raíz del repositorio.

---
