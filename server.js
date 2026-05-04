const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;

const mimeTypes = {
  '.html': 'text/html',
  '.css':  'text/css',
  '.js':   'application/javascript',
  '.png':  'image/png',
  '.jpg':  'image/jpeg',
  '.ico':  'image/x-icon',
};

http.createServer((req, res) => {
  let urlPath = req.url.split('?')[0];

  if (urlPath === '/' || urlPath === '/index.html' || urlPath === '/s') {
    urlPath = '/index.html';
  } else if (urlPath === '/detail' || urlPath === '/detail.html') {
    urlPath = '/detail.html';
  } else {
    // Sab unknown routes index.html par redirect
    res.writeHead(302, { 'Location': '/index.html' });
    res.end();
    return;
  }

  const filePath = path.join(__dirname, urlPath);
  const ext = path.extname(filePath);
  const contentType = mimeTypes[ext] || 'text/plain';

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/html' });
      res.end('<h2>404 - Page Not Found</h2><a href="/">Go to Home</a>');
      return;
    }
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  });

}).listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
  console.log(`Open: http://localhost:${PORT}/index.html`);
});
