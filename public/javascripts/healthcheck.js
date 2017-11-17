var app = angular.module('healthCheckApp', []);

app.controller('appHealthCtrl', function($scope, $http, styleService) {
    $http.get("/system-info/appHealth").then(
        function(response){
            $scope.data = response.data
            $scope.itemIconStyleClass = styleService.itemIconStyleClass;
            $scope.tableRowStyleClass = styleService.tableRowStyleClass;
            $scope.itemStatus = styleService.itemStatus;
        }
    )
});


app.controller('integrationHealthCtrl', function($scope, $http, styleService) {
    $http.get("/system-info/integrationHealth").then(
        function(response) {
            $scope.data = response.data;
            $scope.itemIconStyleClass = styleService.itemIconStyleClass;
            $scope.tableRowStyleClass = styleService.tableRowStyleClass;
            $scope.itemStatus = styleService.itemStatus;
        }
    )
});

app.factory('styleService', function () {
    return {
        itemIconStyleClass: function (isOk) {
            if(isOk){
                return "glyphicon glyphicon-ok-circle";
            } else {
                return "glyphicon glyphicon-exclamation-sign";
            }
        },
        tableRowStyleClass: function(isOk) {
            if(isOk){
                return "";
            } else {
                return "alert alert-danger";
            }
        },
        itemStatus: function(isOk) {
            if(isOk){
                return "OK";
            } else {
                return "NEEDS ATTENTION";
            }
        }
    }
});
