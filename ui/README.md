# Terrakube UI

## Compile

Define the following environment variables in your local machine:

|Name                         | Description                                                          |
|:----------------------------|----------------------------------------------------------------------|
|REACT_CONFIG_TERRAKUBE_URL   | Terrakube API URL Example: https://someURL/terrakube-api/api/v1/     | 
|REACT_CONFIG_CLIENT_ID       | Dex Issuer Application ID                                            |
|REACT_CONFIG_AUTHORITY       | Dex Issuer (From http://dexURl/.well-knownopenid-configuration)      |
|REACT_CONFIG_REDIRECT        | Redirct URL. Example: http://localhost:3000                          |
|REACT_CONFIG_REGISTRY_URI    | Terrakube Registry url. Example: https://someurl/open-registry/      |
|REACT_CONFIG_SCOPE           | Dex Scope (email openid profile offline_access groups)               |

Run the script to generate .env file in your local

```bash
./setupEnv.sh
```

The above script will generate a .env file with the following structure:

```
REACT_APP_TERRAKUBE_API_URL=https://terrakubeUrl/api/v1/
REACT_APP_CLIENT_ID=terrakube-app
REACT_APP_AUTHORITY=https://dexUrl.com
REACT_APP_REDIRECT_URI=http://localhost:3000
REACT_APP_REGISTRY_URI=https://registryUrl.com
REACT_APP_SCOPE=email openid profile offline_access groups
```

> Values are only examples

Run the script env.sh to generate static configuration file env-config.js inside the public folder, the UI will take the values from that static file at run time.

> For a better explanation of how env-config.js and env.sh load the values at running time please refer to the following [documentation](https://www.freecodecamp.org/news/how-to-implement-runtime-environment-variables-with-create-react-app-docker-and-nginx-7f9d42a91d70/)

Build/Start the application:

```bash
yarn install
yarn dev
```

## Docker Support

### Build Image

```bash
docker build -t terrakube-ui:latest  .
```

> If you are a Windows user and face the following error when building the UI ***Bash script and /bin/bash^M: bad interpreter: No such file or directory*** run the following command to fix the issue ***sed -i -e 's/\r$//' env.sh***

> Make sure to run the script  **setupEnv.sh** first to generate the **.env** file

### Run the image with Docker Compose

```dockerfile
version: "3.8"
services:
  terrakube-ui:
    image: terrakube-ui:latest
    container_name: terrakube-ui
    volumes:
      - ./env-config.js:/app/env-config.js
    ports:
      - 8080:8080 
```