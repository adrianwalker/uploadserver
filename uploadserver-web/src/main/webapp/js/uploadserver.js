var LIST = "list";
var PREVIEW = "preview";
var VIEW = "view";

var REST_BASE_URL = "/uploadserver-rest/uploadserver/filesystem/";

var LIST_NAV_TEMPLATE = $("#list-nav-template").html();
var LIST_PREVIEW_TEMPLATE = $("#list-preview-template").html();

var VIEW_NAV_TEMPLATE = $("#view-nav-template").html();
var VIEW_IMAGE_TEMPLATE = $("#view-image-template").html();

class RestParameters {

  constructor(hash) {

    this.hash = decodeURIComponent(hash);
  }

  url() {
    return this.hash.substr(1);
  }

  service() {

    var elements = this.url().split("/");
    return elements[0];
  }

  path() {
    var elements = this.url().split("/");
    return elements.slice(1, elements.length).join("/");
  }

  parentPath() {
    var elements = this.url().split("/");
    return elements.slice(1, elements.length - 1).join("/");
  }

  toString() {
    return this.hash;
  }
}

class HomeState {

  constructor() {
    this.html = null;
  }

  go() {
    var username = $("#username").val();
    $("#go").attr("href", "#" + LIST + "/" + username);
  }
}

class ListState {

  constructor() {
    this.files = [];
    this.pathIndex = new Map();
    this.position = 0;
    this.pageStart = 0;
    this.pageEnd = 20;
    this.pageSize = 20;
    this.html = null;
  }

  page() {
    return this.files.slice(this.pageStart, this.pageEnd);
  }

  hasMore() {
    return this.files.length > this.pageEnd;
  }

  nextPage() {
    this.pageStart += this.pageSize;
    this.pageEnd += this.pageSize;
  }
}

class ViewState {

  constructor() {
    this.files = [];
    this.pathIndex = new Map();
    this.html = null;
  }

  prefetchPath(path) {

    var url = REST_BASE_URL + VIEW + '/' + path;
    return VIEW_IMAGE_TEMPLATE.replace("{url}", url);
  }

  nextPath(path) {

    var nextIndex = this.pathIndex.get(path) + 1;
    var nextFile = this.files[nextIndex];

    if (nextFile) {
      $("#view-prefetch-next").html(this.prefetchPath(nextFile.path));
      return nextFile.path;
    } else {
      return path;
    }
  }

  previousPath(path) {

    var previousIndex = this.pathIndex.get(path) - 1;
    var previousFile = this.files[previousIndex];

    if (previousFile && !previousFile.directory) {
      $("#view-prefetch-previous").html(this.prefetchPath(previousFile.path));
      return previousFile.path;
    } else {
      return path;
    }
  }
}

class UploadServer {

  constructor() {
    this.states = {};
  }

  putState(restParameters, state) {
    this.states[restParameters] = state;
  }

  getState(restParameters) {
    return this.states[restParameters];
  }

  hasState(restParameters) {
    return restParameters in this.states;
  }

  ajax(restParameters) {

    var restUrl = REST_BASE_URL + restParameters;

    return $.ajax({

      url: restUrl,
      dataType: "json",
      type: "GET"

    });
  }

  sortFiles(unsortedFiles, sortFunction) {

    var directories = [];
    var files = [];

    for (var i = 0; i < unsortedFiles.length; i++) {

      if (unsortedFiles[i].directory) {
        directories.push(unsortedFiles[i]);
      } else {
        files.push(unsortedFiles[i]);
      }
    }

    var directories = directories.sort(sortFunction);
    var files = files.sort(sortFunction);

    return directories.concat(files);
  }

  pathIndex(files) {

    var pathIndex = new Map();

    for (var index = 0; index < files.length; index++) {

      var path = files[index].path;
      pathIndex.set(path, index);
    }

    return pathIndex;
  }
}

function visible(element) {

  var elementTop = element.offset().top;
  var elementBottom = elementTop + element.outerHeight();

  var viewportTop = $(window).scrollTop();
  var viewportBottom = viewportTop + $(window).innerHeight();

  return (elementBottom > viewportTop) && (elementTop < viewportBottom);
}

function nameSort(file1, file2) {

  var fileName1 = file1.name.toUpperCase();
  var fileName2 = file2.name.toUpperCase();

  if (fileName1 < fileName2) {
    return -1;
  }

  if (fileName1 > fileName2) {
    return 1;
  }

  return 0;
}

function newState(restParameters) {

  var service = restParameters.service();

  if (LIST === service) {

    newListState(restParameters);

  } else if (VIEW === service) {

    newViewState(restParameters);

  } else {

    newHomeState(restParameters);
  }
}

function newHomeState(restParameters) {

  var homeState = new HomeState();
  app.putState(restParameters, homeState);

  $("#go").click(function () {
    homeState.go();
  });

  $("#view").hide();
  $("#list").hide();
  $("#home").show();

  homeState.html = $("#home").html();
}

function newListState(restParameters) {

  var listState = new ListState();
  app.putState(restParameters, listState);

  return app.ajax(restParameters.url()).done(function (data) {

    listState.files = app.sortFiles(data.files, nameSort);
    listState.pathIndex = app.pathIndex(listState.files);

    $("#list-nav").empty();
    if (restParameters.parentPath()) {
      appendListNav(restParameters);
    }

    $("#list-previews").empty();
    appendListPreviews(restParameters);

    $("#view").hide();
    $("#home").hide();
    $("#list").show();

    listState.html = $("#list").html();
  });
}

function newViewState(restParameters) {

  var viewState = new ViewState();
  app.putState(restParameters, viewState);

  var parent = restParameters.parentPath();
  var listRestParameters = new RestParameters('#' + LIST + '/' + parent);

  if (app.hasState(listRestParameters)) {

    var listState = app.getState(listRestParameters);
    viewState.files = listState.files;
    viewState.pathIndex = listState.pathIndex;

    $("#view-nav").empty();
    appendViewNav(restParameters);

    $("#view-image").empty();
    appendViewImage(restParameters);

    $("#list").hide();
    $("#home").hide();
    $("#view").show();

    viewState.html = $("#view").html();

  } else {

    newListState(listRestParameters).then(function () {
      newViewState(restParameters);
    });
  }
}

function applyState(restParameters) {

  var service = restParameters.service();

  if (LIST === service) {

    applyListState(restParameters);

  } else if (VIEW === service) {

    applyViewState(restParameters);

  } else {

    applyHomeState(restParameters);
  }
}

function applyHomeState(restParameters) {

  var homeState = app.getState(restParameters);

  $("#home").html(homeState.html);
  $("#view").hide();
  $("#list").hide();
  $("#home").show();

  $(window).scrollTop(homeState.position);
}

function applyListState(restParameters) {

  var listState = app.getState(restParameters);

  $("#list").html(listState.html);
  $("#view").hide();
  $("#home").hide();
  $("#list").show();

  $(window).scrollTop(listState.position);

}

function applyViewState(restParameters) {

  var viewState = app.getState(restParameters);

  $("#view").html(viewState.html);
  $("#list").hide();
  $("#home").hide();
  $("#view").show();
}

function more(restParameters) {

  var listState = app.getState(restParameters);

  listState.nextPage();

  appendListPreviews(restParameters);

  listState.html = $("#list").html();
}

function appendViewNav(restParameters) {

  var viewState = app.getState(restParameters);
  var path = restParameters.path();
  var parent = "#" + LIST + '/' + restParameters.parentPath();
  var next = "#" + VIEW + '/' + viewState.nextPath(path);
  var previous = "#" + VIEW + '/' + viewState.previousPath(path);

  $("#view-nav").append(VIEW_NAV_TEMPLATE
          .replace("{parent}", parent)
          .replace("{next}", next)
          .replace("{previous}", previous));
}

function appendViewImage(restParameters) {

  var url = REST_BASE_URL + restParameters.url();

  $("#view-image").append(VIEW_IMAGE_TEMPLATE.replace("{url}", url));
}

function appendListNav(restParameters) {

  var parent = "#" + LIST + '/' + restParameters.parentPath();

  $("#list-nav").append(LIST_NAV_TEMPLATE.replace("{parent}", parent));
}

function appendListPreviews(restParameters) {

  var listState = app.getState(restParameters);
  var page = listState.page();

  for (var i = 0; i < page.length; i++) {
    appendListPreview(page[i]);
  }

  if (listState.hasMore() && visible($("#list-more"))) {
    more(restParameters);
  }
}

function appendListPreview(file) {

  if (file.directory) {
    var url = "#" + LIST + '/' + file.path;
    var previewUrl = "img/album.png";
  } else {
    var url = "#" + VIEW + '/' + file.path;
    var previewUrl = REST_BASE_URL + PREVIEW + '/' + file.path;
  }

  $("#list-previews").append(LIST_PREVIEW_TEMPLATE
          .replace("{name}", file.name)
          .replace("{url}", url)
          .replace("{previewUrl}", previewUrl));
}

var app = new UploadServer();

$(window).on("load", function () {

  var restParameters = new RestParameters(window.location.hash);
  newState(restParameters);

  $("#more").click(function () {
    var restParameters = new RestParameters(window.location.hash);
    more(restParameters);
  });
});

$(window).on("hashchange", function () {

  var restParameters = new RestParameters(window.location.hash);

  if (app.hasState(restParameters)) {
    applyState(restParameters);
  } else {
    newState(restParameters);
  }
});

$(window).on("scroll resize", function () {

  var restParameters = new RestParameters(window.location.hash);
  var service = restParameters.service();

  if (LIST === service && visible($("#list-more"))) {
    more(restParameters);
  }

  app.getState(restParameters).position = $(window).scrollTop();
});
