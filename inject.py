import re

with open('detail.html', 'r', encoding='utf-8', errors='ignore') as f:
    c = f.read()

# Remove "Only X left in stock" text
c = re.sub(r'Only \d+ left in stock[^<]*', '', c)

# Remove delivery location error message
c = re.sub(r'Sorry, your selected delivery location[^<]*', '', c)
c = re.sub(r'Please choose a different delivery location[^<]*', '', c)
c = re.sub(r'purchase from another seller\.[^<]*', '', c)

# Remove old injected scripts
c = re.sub(r'<script>\s*function applyData.*?</script>', '', c, flags=re.DOTALL)
c = re.sub(r'<script>\s*window\.addEventListener\(\'load\'.*?</script>', '', c, flags=re.DOTALL)

# Inject script before </body>
script = """<script>
window.addEventListener('load', function(){
  var price = localStorage.getItem('p_price') || '';
  var title = localStorage.getItem('p_title') || '';
  var img   = localStorage.getItem('p_img')   || '';

  if(title){
    var el = document.getElementById('productTitle');
    if(el) el.textContent = title;
  }
  if(price){
    var num = price.replace(/[^0-9]/g,'');
    document.querySelectorAll('.a-price-whole').forEach(function(el){
      el.childNodes[0].nodeValue = num;
    });
    document.querySelectorAll('.a-offscreen').forEach(function(el){
      el.textContent = price;
    });
  }
  if(img){
    var el = document.getElementById('landingImage');
    if(el){ el.src=img; el.srcset=''; el.removeAttribute('data-a-dynamic-image'); }
  }
});
document.addEventListener('submit',function(e){e.preventDefault();},true);
</script>"""

c = c.replace('</body>', script + '</body>', 1)

with open('detail.html', 'w', encoding='utf-8') as f:
    f.write(c)

print("Done!")
