/* Modalidad por horario: publicación, resumen y privacidad de las reservas. */
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../web/app.js'),'utf8');
function interfaz(rol='estudiante',carnet='E1'){
 const nodes=new Map(),events={};
 const document={querySelector(id){if(!nodes.has(id))nodes.set(id,{value:'',textContent:'',innerHTML:'',showModal(){this.open=true;},close(){this.open=false;}});return nodes.get(id);},addEventListener(name,callback){const previous=events[name];events[name]=previous?async e=>{await previous(e);await callback(e);}:callback;}};
 const context=vm.createContext({document,window:{addEventListener(){}}});
 vm.runInContext(source.replace(/\ninit\(\);\s*$/,'\n')+'\nglobalThis.ui={booking,requestCard,updateSummary,modalityFields};',context);
 context.fixture={usuario:{rol,carnet},cursos:[{codigo:'C1',nombre:'POO'}],tutores:[{carnet:'T1',nombre:'María',tarifa:40,beca:false,cursos:['C1'],horarios:[{id:'H1',fecha:'2026-12-01',inicio:'10:00',fin:'11:30',duracion:1.5,disponible:true,modalidad:'PRESENCIAL',ubicacion:'B-205'}]}],preguntas:[],sesiones:[],solicitudes:[]};
 vm.runInContext('state=fixture',context);
 return {ui:context.ui,nodes,events,context};
}
function solicitud(modalidad,estado='PENDIENTE'){
 return {id:1,curso:'C1',nombreCurso:'POO',tutor:'T1',nombreTutor:'María',nombreEstudiante:'Ana',fecha:'2026-12-01',inicio:'10:00',fin:'11:30',precio:60,tema:'Herencia',modalidad,estado};
}
test('El tutor publica modalidad obligatoria; el alumno elige un bloque',()=>{
 const {ui}=interfaz();
 const html=ui.booking(new URLSearchParams());
 assert.doesNotMatch(html,/<select[^>]*name="modalidad"/);
 assert.match(html,/modalidad y ubicación corresponden al horario/);
 const campos=ui.modalityFields();
 assert.match(campos,/<select[^>]*name="modalidad"[^>]*required/);
 assert.match(campos,/<input[^>]*name="ubicacion"[^>]*required/);
 assert.match(campos,/>Virtual<\/option>/);
 assert.match(campos,/>Presencial<\/option>/);
});
test('El resumen usa condiciones del bloque y conserva el precio',()=>{
 const {ui,nodes,context}=interfaz();
 ui.updateSummary();nodes.get('#curso').value='C1';nodes.get('#tutor').value='T1';
 ui.updateSummary();nodes.get('#horario').value='H1';ui.updateSummary();
 let html=nodes.get('#resumen-reserva').innerHTML;
 assert.match(html,/Presencial/);assert.match(html,/B-205/);assert.match(html,/60\.00/);
 context.fixture.tutores[0].horarios[0].modalidad='EN_LINEA';
 context.fixture.tutores[0].horarios[0].ubicacion=null;
 ui.updateSummary();html=nodes.get('#resumen-reserva').innerHTML;
 assert.match(html,/Virtual/);assert.match(html,/enlace estará en Mis solicitudes/);
});
test('El alumno y el tutor ven la modalidad y mantienen sus acciones',()=>{
 for(const [value,label] of [['EN_LINEA','Virtual'],['PRESENCIAL','Presencial'],[null,'Pendiente de completar']]){
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
