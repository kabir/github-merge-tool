'use strict';

/* App Module */

var mergeToolApp = angular.module('MergeToolApp', [
  'ngRoute',
  /*
  'mergeToolAnimations',
*/
  'mergeToolControllers',
  /*
  'mergeToolFilters',
  'mergeToolServices'
  */
]);

mergeToolApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/home', {
        templateUrl: 'partials/home.html',
        controller: 'HomeCtrl'
      }).
      when('/login', {
        templateUrl: 'partials/login.html',
        controller: 'LoginCtrl'
      }).
      otherwise({
        redirectTo: '/home'
      });
  }]);
