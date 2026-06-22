// scene.js — renderer, camera, lights, OrbitControls, resize, and the render loop.
//
// Render strategy: a persistent requestAnimationFrame loop calls the (cheap)
// controls.update() every frame so OrbitControls damping always settles, but the
// (expensive) renderer.render() is GATED behind a dirty flag. So when nothing is
// moving we don't redraw 256 meshes 60x/sec. invalidate() marks one frame dirty;
// OrbitControls' 'change' event invalidates automatically while you drag/coast.
(function (NS) {
  var scene, camera, renderer, controls, canvas;
  var needsRender = true;
  var animating = false; // true while piece tweens are running (boardView sets this)
  var frameCbs = [];     // per-frame callbacks (e.g. tween updater), called with performance.now()

  // Register a callback invoked once per animation frame (before the gated render).
  NS.onEachFrame = function (fn) { frameCbs.push(fn); };

  NS.initScene = function (canvasEl) {
    canvas = canvasEl;
    renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.sortObjects = true;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0a0d13);

    camera = new THREE.PerspectiveCamera(55, 1, 0.1, 500);
    camera.position.set(11, 10, 14);

    controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.target.set(0, 0, 0);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.minDistance = 4;
    controls.maxDistance = 80;
    controls.addEventListener('change', NS.invalidate);

    // Lighting: soft ambient + a warm key light + a cool fill so the two armies
    // (warm vs cool materials) stay readable from any orbit angle.
    scene.add(new THREE.AmbientLight(0xffffff, 0.6));
    var key = new THREE.DirectionalLight(0xfff2e0, 0.85); key.position.set(12, 20, 10); scene.add(key);
    var fill = new THREE.DirectionalLight(0x9bb8ff, 0.4); fill.position.set(-14, -8, -12); scene.add(fill);

    NS.scene = scene; NS.camera = camera; NS.renderer = renderer; NS.controls = controls;

    window.addEventListener('resize', NS.resize);
    NS.resize();
    requestAnimationFrame(loop);
  };

  NS.invalidate = function () { needsRender = true; };
  NS.setAnimating = function (v) { animating = !!v; if (animating) needsRender = true; };

  NS.resize = function () {
    var w = canvas.clientWidth || window.innerWidth;
    var h = canvas.clientHeight || window.innerHeight;
    renderer.setSize(w, h, false);
    camera.aspect = w / Math.max(1, h);
    camera.updateProjectionMatrix();
    needsRender = true;
  };

  function loop() {
    requestAnimationFrame(loop);
    controls.update();                 // cheap; keeps damping progressing
    var now = (typeof performance !== 'undefined') ? performance.now() : Date.now();
    for (var i = 0; i < frameCbs.length; i++) frameCbs[i](now);
    if (needsRender || animating) {
      renderer.render(scene, camera);  // expensive; only when dirty/animating
      needsRender = false;
    }
  }
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
