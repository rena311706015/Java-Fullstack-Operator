> *Quarkus 框架是一個針對Kubernetes、Serverless、微服務和雲原生應用程式開發而設計的現代 Java 框架*

請先安裝 Quarkus CLI https://quarkus.io/get-started/

### Get Start
1. 啟動 minikube

    `minikube start`
    
3. 啟用 Ingress  
    1. `minikube addons enable ingress`  
    2. `minikube ip` 複製 ip 位置後  
    3. `sudo nano /etc/hosts` 最底下新增  
        `<minikube-ip> quarkusapp.minikube.local`
        
4. 切換 docker 為 minikube docker 
    
    `eval $(minikube docker-env)`
    
5. 載入前端和後端的 image
    
    這邊我把之前的 image 推到 Dockerhub 了， 請直接 pull
    
    `docker pull renawang0913/backend-demo`
    
    `docker pull renawang0913/frontend-demo`
    
6. 打包 Operator的 image
    
    `mvn clean package`
    
    完成後會生成 target 資料夾，並根據 application.properties 的設定在 docker env 中產生 operator 的 Image
    
    <img width="992" height="130" alt="image (15)" src="https://github.com/user-attachments/assets/25611bb3-7160-4b8a-9c81-afbe616a6c72" />
    
7. 在 mvn 生成的 target/kubernetes/kubernetes.yml 中額外加上一些權限
    
    ```yaml
    apiVersion: rbac.authorization.k8s.io/v1
    kind: ClusterRole
    metadata:
      name: quarkusappreconciler-managed-resources
    rules:
      - apiGroups: [""]
        resources:
          - services
          - secrets
        verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
    
      - apiGroups: ["apps"]
        resources:
          - deployments
        verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
    
      - apiGroups: ["mysql.oracle.com"]
        resources:
          - innodbclusters
        verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
    
      - apiGroups: ["autoscaling"]
        resources: ["horizontalpodautoscalers"]
        verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
      
      - apiGroups: ["networking.k8s.io"]
        resources: 
          - ingresses
        verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: ClusterRoleBinding
    metadata:
      name: quarkusappreconciler-managed-resources-binding
    roleRef:
      apiGroup: rbac.authorization.k8s.io
      kind: ClusterRole
      name: quarkusappreconciler-managed-resources
    subjects:
      - kind: ServiceAccount
        name: quarkus-app-operator
        namespace: default
    ---
    ```
    
8. 確保 docker images 有出現 frontend、backend、operator 的 image 後，**依順序部屬資源**
    1. `kubectl apply -f mysql-secret.yml`
    2. `kubectl apply -f https://raw.githubusercontent.com/mysql/mysql-operator/*trunk*/deploy/deploy-crds.yaml`
    3. `kubectl apply -f https://raw.githubusercontent.com/mysql/mysql-operator/*trunk*/deploy/deploy-operator.yaml`
    4. `kubectl apply -f quarkusapp-crd.yml`
    5. `kubectl apply -f quarkus-app-operator/target/kubernetes/kubernetes.yml`
    6. 確認 mysql-operator 啟動後 `kubectl apply -f quarkusapp-cr.yml`

    也可以利用 Helm 一次就能成功部屬所有資源，可以參考這個 Repo  
    [![GitHub Repo](https://img.shields.io/badge/GitHub-Fullstack--Operator--Helm--Chart-181717?logo=github)](https://github.com/rena311706015/Fullstack-Operator-Helm-Chart)
    
10. 預期應該要有這些 pod (-A 查詢所有 Namespace)

     <img width="1015" height="523" alt="image (12)" src="https://github.com/user-attachments/assets/99acc86f-a694-45f1-8b4f-0f76b10cdd6d" />
    
11. 一次查詢所有資源類型 `kubectl get deployments,services,secrets,configmaps,hpa,ingress`

    <img width="1556" height="809" alt="image (13)" src="https://github.com/user-attachments/assets/3e978d6a-4fb5-4109-9906-9eeac1aaa99e" />
    
12. 因為我們有啟用 Ingress，所以可以直接在搜尋欄打上 https://quarkusapp.minikube.local
    
    <img width="429" height="159" alt="image (14)" src="https://github.com/user-attachments/assets/f2adc28b-4a12-43cd-860e-3c5e079a6157" />

13. 查詢 SQL 
    
    `kubectl run --rm -it myshell \
    --image=container-registry.oracle.com/mysql/community-operator \
    -- mysqlsh root@my-production-app-db-cluster:6446/mysql --password=asuspassword --sql`
