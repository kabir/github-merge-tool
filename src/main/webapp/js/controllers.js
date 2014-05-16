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
  

