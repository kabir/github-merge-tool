'use strict';

/* Controllers */

var mergeToolControllers = angular.module('mergeToolControllers', []);

mergeToolControllers.controller('HeaderCtrl', ['$scope',
  function($scope) {
    //TODO Restify
    $scope.header = {
       loggedin:false,
       admin:true
    }
  }]);

mergeToolControllers.controller('HomeCtrl', ['$scope', 
  function($scope) {
  }]);
  
mergeToolControllers.controller('LoginCtrl', ['$scope', 
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
  

