'use strict';

/* Controllers */

//var mergeToolControllers = angular.module('mergeToolControllers', []);
var mergeToolApp = angular.module('MergeToolApp', []);

//mergeToolControllers.controller('HeaderCtrl', ['$scope',
mergeToolApp.controller('HeaderCtrl', ['$scope',
  function($scope) {
    //TODO Restify
    $scope.header = {
       loggedin:false,
       admin:false,
       
    }
  }]);

//mergeToolControllers.controller('HomeCtrl', ['$scope',
mergeToolApp.controller('HomeCtrl', ['$scope', 
  function($scope) {
  }]);
  
//mergeToolControllers.controller('LoginCtrl', ['$scope',
mergeToolApp.controller('LoginCtrl', ['$scope', 
  function($scope) {
      $scope.failed = false;
      $scope.user = {};
      
      $scope.login = function(user) {
          var loginsuccess = false;
          if (loginsuccess) {
          
          } else {
            $scope.failed=true;
          }
      }
  }]);
  

