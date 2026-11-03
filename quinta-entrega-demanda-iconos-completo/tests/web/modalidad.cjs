/* Modalidad: elección obligatoria, resumen y decisión del tutor con el flujo existente. */
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../web/app.js'),'utf8');
function interfaz(rol='estudiante',carnet='E1'){
 const nodes=new Map(),events={};
 const document={querySelector(id){if(!nodes.has(id))nodes.set(id,{value:'',textContent:'',innerHTML:'',showModal(){this.open=true;},close(){this.open=false;}});return nodes.get(id);},addEventListener(name,callback){events[name]=callback;}};
 const context=vm.createContext({document,window:{addEventListener(){}}});
 vm.runInContext(source.replace(/\ninit\(\);\s*$/,'\n')+'\nglobalThis.ui={booking,requestCard,updateSummary};',context);
 context.fixture={usuario:{rol,carnet},cursos:[{codigo:'C1',nombre:'POO'}],tutores:[{carnet:'T1',nombre:'María',tarifa:40,beca:false,cursos:['C1'],horarios:[{id:'H1',fecha:'2026-12-01',inicio:'10:00',fin:'11:30',duracion:1.5,disponible:true}]}],preguntas:[],sesiones:[],solicitudes:[]};
 vm.runInContext('state=fixture',context);
 return {ui:context.ui,nodes,events,context};
}
function solicitud(modalidad,estado='PENDIENTE'){
 return {id:1,curso:'C1',nombreCurso:'POO',tutor:'T1',nombreTutor:'María',nombreEstudiante:'Ana',fecha:'2026-12-01',inicio:'10:00',fin:'11:30',precio:60,tema:'Herencia',modalidad,estado};
}
test('La solicitud exige elegir en línea o presencial con los controles existentes',()=>{
 const html=interfaz().ui.booking(new URLSearchParams());
 assert.match(html,/<label for="modalidad">5. Modalidad de la tutoría<\/label>/);
 assert.match(html,/<select id="modalidad" name="modalidad" required>/);
 assert.match(html,/<option value="">Selecciona una modalidad<\/option>/);
 assert.match(html,/<option value="EN_LINEA">En línea<\/option>/);
 assert.match(html,/<option value="PRESENCIAL">Presencial<\/option>/);
});
test('Cambiar modalidad actualiza el resumen y conserva el precio',()=>{
 const {ui,nodes,events}=interfaz();
 ui.updateSummary();
 nodes.get('#curso').value='C1';nodes.get('#tutor').value='T1';
 ui.updateSummary();nodes.get('#horario').value='H1';
 ui.updateSummary();
 for(const [value,label] of [['EN_LINEA','En línea'],['PRESENCIAL','Presencial']]){
  nodes.get('#modalidad').value=value;events.change({target:{id:'modalidad'}});
  const html=nodes.get('#resumen-reserva').innerHTML;
  assert.ok(html.includes('<dt>Modalidad</dt><dd>'+label+'</dd>'));
  assert.match(html,/60\.00/);
 }
});
test('El alumno y el tutor ven la modalidad y mantienen sus acciones',()=>{
 for(const [value,label] of [['EN_LINEA','En línea'],['PRESENCIAL','Presencial'],[null,'No especificada']]){
  const r=solicitud(value);
  const alumno=interfaz().ui.requestCard(r),tutor=interfaz('tutor','T1').ui.requestCard(r);
  for(const html of [alumno,tutor])assert.ok(html.includes('Modalidad: '+label));
  assert.match(alumno,/>Cancelar<\/button>/);assert.doesNotMatch(alumno,/Rechazar tutoría|Aceptar tutoría/);
  assert.match(tutor,/>Aceptar tutoría<\/button>/);assert.match(tutor,/>Rechazar tutoría<\/button>/);
  assert.match(interfaz('tutor','T1').ui.requestCard(solicitud(value,'CONFIRMADA')),/>Cancelar<\/button>/);
  assert.doesNotMatch(interfaz('tutor','T2').ui.requestCard(r),/data-command=/);
  assert.match(interfaz('coordinador','T1').ui.requestCard(r),/>Rechazar tutoría<\/button>/);
 }
});
test('Rechazar pide confirmación y utiliza la cancelación existente',async()=>{
 const {context,events,nodes}=interfaz('tutor','T1');
 context.fixture.solicitudes=[solicitud('EN_LINEA')];
 vm.runInContext('globalThis.calls=[];perform=async(action,data)=>calls.push({action,data})',context);
 const button={dataset:{command:'cancelar',id:'1'}};
 await events.click({target:{closest:()=>button}});
 assert.equal(nodes.get('#confirmar-titulo').textContent,'Rechazar esta tutoría');
 assert.equal(nodes.get('#cancelar-confirmado').textContent,'Sí, rechazar tutoría');
 assert.equal(context.calls.length,0);
 await events.click({target:{closest:()=>({id:'cancelar-confirmado',dataset:{}})}});
 assert.equal(context.calls.length,1);assert.equal(context.calls[0].action,'cancelar');assert.equal(context.calls[0].data.solicitud,'1');
});
