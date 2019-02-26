# Multiple Cloud Storage Manager
Demo application for easy managing different Cloud servises from a single interface.
Allows simultaneous work with different Cloud Drives the same way as if they are hard drives on the computer


Features: 
- Explore Cloud drives, 
- Upload, download, remove, rename files and folders
- Copy and move files and folders between cloud drives 
- Uses OAuth2 protocol for authorization, so no need to enter password for each Cloud Storage

Supports Dropbox, Google Drive and Microsoft OneDrive

# Interface

![screenshot from 2019-02-25 11-45-04](https://user-images.githubusercontent.com/8477052/53364485-bd8f3c00-38f3-11e9-8479-8e485641209e.png)


<img src="https://user-images.githubusercontent.com/8477052/53360249-e4e10b80-38e9-11e9-838b-39804c06560f.png" width="500">

# Setup

1. Install Java
2. Install MySql
  - `sudo apt install mysql-server`
  - Set user and password: 
 ```
 $ sudo mysql
  
 /*! CREATE USER/ROLE and a password */
 mysql> CREATE USER 'userName'@'localhost' IDENTIFIED BY 'password';
 
 /*! GRANT ALL PRIVILEGED to the user */.
 mysql> GRANT ALL PRIVILEGES ON *.* TO 'userName'@'localhost';
  ```
  3. Install Maven:
  `sudo apt install maven`
  4. Obtain API keys for each Cloud Service:
  - [Dropbox](https://www.dropbox.com/developers/apps/create)
  - [Google Drive](https://console.developers.google.com/start)
  - [Microsoft OneDrive](https://apps.dev.microsoft.com/#/appList)
  5. Clone repository
  `git clone https://github.com/natlg/CloudStorageManager.git`
  6. Create `applications.properties` file
  ```
  cd CloudStorageManager
  cp ./src/main/resources/application.properties.example ./src/main/resources/application.properties
  ```
   Specify parameters: <br>
   `spring.datasource.url` - Url to DataBase (or use default) <br>
   `spring.datasource.username` - Name of MySQL user created on step 2 <br>
   `spring.datasource.password` - User password <br>
   `Clouds keys` - Insert keys and secrets from step 4 
   
   7. Run <br>
      `mvn spring-boot:run  `
   
   
   
   
   
  
  
  
