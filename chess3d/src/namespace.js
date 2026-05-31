// namespace.js — establishes the single global namespace shared by the engine
// (pure logic, also loadable in Node for tests) and the view (browser + THREE).
//
// Every other file is an IIFE that grabs this namespace. Using globalThis means
// the *exact same files* work via <script> in the browser AND via require() in
// Node, because both expose globalThis as the place the namespace lives.
(function () {
  var root = (typeof globalThis !== 'undefined') ? globalThis
           : (typeof window !== 'undefined') ? window : this;
  var NS = root.Chess3D || (root.Chess3D = {});

  // --- Fundamental cube dimensions (shared by engine + view) ---
  NS.SIZE = 8;        // cube edge length
  NS.CELLS = 512;     // SIZE^3

  // --- index <-> (x,y,z) helpers. index = x + 8*y + 64*z ---
  NS.index = function (x, y, z) { return x + (y << 3) + (z << 6); };
  NS.xOf = function (i) { return i & 7; };
  NS.yOf = function (i) { return (i >> 3) & 7; };
  NS.zOf = function (i) { return (i >> 6) & 7; };
  NS.inBounds = function (x, y, z) {
    return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < 8;
  };
})();
