// labels.js — piece identity letters as Sprites. CanvasTexture (text drawn on a 2D
// canvas) is the only file://-safe text path: no FontLoader/TextGeometry (those fetch
// a font JSON and get CORS-blocked under file://), no external glyph images.
// Drawn dark with a light halo so the letter is legible on either side's material.
(function (NS) {
  var matCache = {}; // by letter — sprites can share a material

  function labelMaterial(letter) {
    if (matCache[letter]) return matCache[letter];
    var size = 128;
    var canvas = document.createElement('canvas');
    canvas.width = size; canvas.height = size;
    var ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, size, size);
    ctx.font = 'bold 84px system-ui, Arial, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.lineWidth = 11;
    ctx.lineJoin = 'round';
    ctx.strokeStyle = 'rgba(255,255,255,0.95)'; // light halo for contrast
    ctx.strokeText(letter, size / 2, size / 2 + 4);
    ctx.fillStyle = '#16161d';                   // dark glyph
    ctx.fillText(letter, size / 2, size / 2 + 4);

    var tex = new THREE.CanvasTexture(canvas);
    tex.minFilter = THREE.LinearFilter;
    tex.magFilter = THREE.LinearFilter;
    tex.generateMipmaps = false;
    var mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: true, depthWrite: false });
    matCache[letter] = mat;
    return mat;
  }

  NS.makeLabelSprite = function (type) {
    var sprite = new THREE.Sprite(labelMaterial(NS.PIECE_LETTERS[type]));
    sprite.scale.set(0.5, 0.5, 0.5);
    sprite.position.set(0, 0.5, 0); // hover above the piece body
    sprite.userData.isLabel = true;
    return sprite;
  };
})(globalThis.Chess3D || (globalThis.Chess3D = {}));
