'use strict';

/* App Module */

var mergeToolApp = angular.module('mergeToolApp', [
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
      otherwise({
        redirectTo: '/home'
      });
  }]);
