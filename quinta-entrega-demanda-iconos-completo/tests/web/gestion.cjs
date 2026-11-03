/* Interfaz de gestión: acciones disponibles, formularios y confirmación accesible. */
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../web/app.js'),'utf8');
function interfaz(state){
 const nodes=new Map();
 const document={querySelector(id){if(!nodes.has(id))nodes.set(id,{textContent:'',showModal(){this.open=true;}});return nodes.get(id);},addEventListener(){}};
 const context=vm.createContext({document,window:{addEventListener(){}}});
 vm.runInContext(source.replace(/\ninit\(\);\s*$/,'\n')+'\nglobalThis.ui={tutors,courses,editTutor,professor,confirmOperation};',context);
 context.fixture=state;
 vm.runInContext('state=fixture',context);
 return {ui:context.ui,nodes};
}
function tutor(overrides={}){return {carnet:'T1',nombre:'María',correo:'maria@example.com',anio:3,beca:true,tarifa:0,activo:true,aprobado:true,profesor:false,cursos:['C1'],horarios:[],horasBeca:2,remuneracion:35,...overrides};}
function estado(tutores=[tutor()]){return {usuario:{rol:'coordinador',carnet:'P1'},tutores,cursos:[{codigo:'C1',nombre:'POO',icono:'code'}],cursosEliminados:[],sesiones:[],solicitudes:[]};}
test('Coordinación puede editar o quitar tutores y el perfil docente del profesor',()=>{
 const {ui}=interfaz(estado([tutor(),tutor({carnet:'P1',profesor:true})]));
 const html=ui.tutors();
 assert.match(html,/href="#editar-tutor\?tutor=T1"/);
 assert.match(html,/href="#editar-tutor\?tutor=P1"/);
 assert.match(html,/Quitar profesor/);
 assert.equal((html.match(/data-command="eliminar-tutor"/g)||[]).length,2);
});
test('El formulario preserva identidad fija y muestra correctamente beca o tarifa de pago',()=>{
 let {ui}=interfaz(estado());
 let html=ui.editTutor(new URLSearchParams('tutor=T1'));
 assert.match(html,/data-action="editar-tutor"/);
 assert.match(html,/name="tutor" value="T1"/);
 assert.match(html,/name="carnet"[^>]*readonly/);
 assert.match(html,/id="beca" checked/);
 assert.match(html,/name="tarifa"[^>]*readonly/);
 assert.match(html,/reservas existentes conservan su precio/);
 ({ui}=interfaz(estado([tutor({beca:false,tarifa:45.5})])));
 html=ui.editTutor(new URLSearchParams('tutor=T1'));
 assert.doesNotMatch(html,/id="beca" checked/);
 assert.match(html,/name="tarifa" type="number" value="45.5"/);
 assert.doesNotMatch(html,/name="tarifa"[^>]*readonly/);
});
test('El personal retirado conserva historia y no aparece en aprobaciones ni botones de edición',()=>{
 const {ui}=interfaz(estado([tutor({activo:false})]));
 const html=ui.tutors();
 assert.match(html,/Personal retirado \(1\)/);
 assert.match(html,/Horas beca: 2/);
 assert.doesNotMatch(html,/data-command="eliminar-tutor"/);
 assert.doesNotMatch(html,/data-action="aprobar"/);
 assert.doesNotMatch(html,/href="#editar-tutor/);
 assert.match(ui.editTutor(new URLSearchParams('tutor=T1')),/Perfil no disponible/);
});
test('Eliminar cursos muestra acción en activos y una lista separada de retirados',()=>{
 const state=estado();state.cursosEliminados=[{codigo:'OLD',nombre:'Anterior'}];
 const html=interfaz(state).ui.courses();
 assert.equal((html.match(/data-command="eliminar-curso"/g)||[]).length,1);
 assert.match(html,/Cursos retirados \(1\)/);
 assert.match(html,/OLD · Anterior/);
});
test('Los datos de edición y los nombres retirados escapan HTML',()=>{
 const value='<img src=x onerror=alert(1)>';
 const state=estado([tutor({nombre:value,correo:'" onfocus="alert(1)'})]);
 state.cursosEliminados=[{codigo:'OLD',nombre:value}];
 const {ui}=interfaz(state);
 for(const html of [ui.tutors(),ui.editTutor(new URLSearchParams('tutor=T1')),ui.courses()]){
  assert.doesNotMatch(html,/<img/);assert.match(html,/&lt;img/);
 }
});
test('La confirmación usa texto y mantiene la cancelación de tutorías existente',()=>{
 const {ui,nodes}=interfaz(estado());
 ui.confirmOperation('Eliminar curso','Curso <img src=x>','Sí, eliminar curso',()=>{});
 assert.equal(nodes.get('#confirmar-descripcion').textContent,'Curso <img src=x>');
 assert.equal(nodes.get('#cancelar-confirmado').textContent,'Sí, eliminar curso');
 assert.equal(nodes.get('#confirmacion').open,true);
 ui.confirmOperation('Cancelar esta tutoría','Se libera el horario','Sí, cancelar tutoría',()=>{});
 assert.equal(nodes.get('#conservar').textContent,'Conservar tutoría');
});
test('Un profesor retirado conserva el panel de coordinación y ve el estado de su perfil docente',()=>{
 const {ui}=interfaz(estado([tutor({carnet:'P1',activo:false,profesor:true})]));
 const html=ui.professor(new URLSearchParams());
 assert.match(html,/acceso de coordinación sigue activo/);
 assert.doesNotMatch(html,/data-action="horario"/);
});
