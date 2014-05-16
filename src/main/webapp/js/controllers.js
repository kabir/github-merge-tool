'use strict';

/* Controllers */

var mergeToolControllers = angular.module('mergeToolControllers', []);

mergeToolControllers.controller('HeaderCtrl', ['$scope',
  function($scope) {
    $scope.loggedin = true; //TODO Restify
  }]);

mergeToolControllers.controller('HomeCtrl', ['$scope', 
  function($scope) {
  }]);
  

