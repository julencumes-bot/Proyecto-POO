/* Pruebas del HTML generado. No sustituyen las pruebas visuales con Playwright. */
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'../../web/app.js'),'utf8');
function interfaz(state){
 const context=vm.createContext({document:{querySelector:()=>null,addEventListener(){}},window:{addEventListener(){}}});
 vm.runInContext(source.replace(/\ninit\(\);\s*$/,'\n')+'\nglobalThis.ui={demand,courses,courseCards,courseIconPicker};',context);
 context.fixture=state;
 vm.runInContext('state=fixture',context);
 return context.ui;
}
function estado(cursos=[],demanda=[]){return {usuario:{rol:'coordinador'},cursos,demanda,tutores:[]};}
test('La demanda vacía invita a registrar el primer curso',()=>{
 const html=interfaz(estado()).demand();
 assert.match(html,/Aún no hay cursos/);
 assert.match(html,/href="#cursos"/);
 assert.doesNotMatch(html,/<table/);
});
test('La demanda presenta curso, código, conteos, temas y total en una tabla accesible',()=>{
 const rows=[{codigo:'M1',nombre:'Matemáticas',icono:'math',cantidad:3,temas:[{tema:'integrales',cantidad:2},{tema:'matrices',cantidad:1}]},{codigo:'P1',nombre:'Programación',icono:'code',cantidad:0,temas:[]}];
 const html=interfaz(estado([],rows)).demand();
 assert.match(html,/<table class="demand-table">/);
 assert.equal((html.match(/scope="col"/g)||[]).length,4);
 assert.equal((html.match(/class="demand-count"/g)||[]).length,2);
 assert.match(html,/M1/);assert.match(html,/Matemáticas/);assert.match(html,/integrales/);
 assert.match(html,/aria-label="2 solicitudes">2/);
 assert.match(html,/class="demand-count">0/);assert.match(html,/Sin solicitudes/);
 assert.match(html,/<strong>3<\/strong>/);assert.match(html,/role="region"/);
 assert.doesNotMatch(html,/<pre/);
});
test('Los nombres, códigos y temas se muestran como texto y nunca ejecutan HTML',()=>{
 const value='<img src=x onerror=alert(1)> & "prueba"';
 const html=interfaz(estado([],[{codigo:value,nombre:value,icono:'book',cantidad:1,temas:[{tema:value,cantidad:1}]}])).demand();
 assert.doesNotMatch(html,/<img/);assert.match(html,/&lt;img/);assert.match(html,/&amp;/);assert.match(html,/&quot;prueba&quot;/);
});
test('El formulario ofrece doce iconos con etiqueta y una selección inicial',()=>{
 const html=interfaz(estado()).courseIconPicker();
 assert.equal((html.match(/type="radio"/g)||[]).length,12);
 assert.equal((html.match(/ checked/g)||[]).length,1);
 assert.match(html,/<legend>Icono del curso<\/legend>/);
 assert.match(html,/name="icono" value="book" checked/);
 for(const id of ['book','code','math','nodes','atom','flask','leaf','globe','landmark','palette','music','money']){
  assert.ok(html.includes('for="icono-'+id+'"'));assert.ok(html.includes('id="icono-'+id+'"'));
 }
});
test('El icono elegido prevalece sobre el nombre en coordinación y en tarjetas del estudiante',()=>{
 const course={codigo:'M1',nombre:'Matemáticas',icono:'palette'};
 const ui=interfaz(estado([course]));
 assert.match(ui.courses(),/form data-action="curso"/);
 const html=ui.courseCards([course]);
 assert.match(html,/data-icon="palette"/);assert.doesNotMatch(html,/data-icon="math"/);
 assert.match(ui.courses(),/class="sticker"[^>]*><svg[^>]*data-icon="palette"/);
});
test('Las tarjetas anteriores sin icono siguen usando la sugerencia por nombre',()=>{
 const html=interfaz(estado()).courseCards([{codigo:'P1',nombre:'Programación'}]);
 assert.match(html,/data-icon="code"/);
});
