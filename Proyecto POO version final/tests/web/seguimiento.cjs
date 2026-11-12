const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../web/app.js'),'utf8').replace(/\ninit\(\);\s*$/,'\n');
function ui(rol='estudiante',carnet='E001'){
 const nodes=new Map();const events={};
 const document={querySelector(id){if(!nodes.has(id))nodes.set(id,{innerHTML:'',value:'',textContent:'',querySelector(){return null;}});return nodes.get(id);},addEventListener(event,fn){(events[event]??=[]).push(fn);}};
 const context=vm.createContext({document,window:{addEventListener(){}},location:{hash:'#inicio'},URLSearchParams,Intl,Date});
 vm.runInContext(source+'\nglobalThis.ui={valoracionSesion,indicadoresTutor,formularioReporte,reporteCard,periodosView,ausenciasView,requestCard,historyCards,profileCards,tutors,profile,navigation,render};',context);
 context.fixture={usuario:{rol,carnet,nombre:'Ana'},cursos:[{codigo:'C1',nombre:'POO'}],cursosEliminados:[],estudiantes:[],solicitudes:[],sesiones:[],preguntas:[],tutores:[{carnet:'T001',nombre:'María',correo:'m@example.com',activo:true,aprobado:true,beca:true,cursos:['C1'],cursados:[],horarios:[],anio:3,valoraciones:[],evaluacionesAcademicas:[],suspensiones:[],strikesConfirmadosSemestre:0}],seguimiento:{hoy:'2026-05-15',periodoActual:'A',periodos:[{id:'A',inicio:'2026-01-12',fin:'2026-05-20'}],misValoraciones:[],miSituacion:{strikesConfirmadosSemestre:0},reportes:[],sanciones:[],resoluciones:[],contadores:[],politica:{}}};
 vm.runInContext('state=fixture',context);return {ui:context.ui,state:context.fixture,context,nodes};
}
function reporte(){return {id:1,solicitud:1,autor:'E001',rolAutor:'estudiante',reportado:'T001',rolReportado:'tutor',periodo:'A',fechaSesion:'2026-05-01',inicio:'10:00',fin:'11:00',curso:'POO',estudiante:'Ana',tutor:'María',fecha:'2026-05-02T12:00:00-06:00',motivo:'<script>alert(1)</script>',evidencia:'<img src=x onerror=alert(1)>',estado:'PENDIENTE',puedeResolver:true,puedeExplicar:true,decisiones:[],explicaciones:[]};}
function solicitud(){return {id:1,estudiante:'E001',nombreEstudiante:'Ana',tutor:'T001',nombreTutor:'María',curso:'C1',nombreCurso:'POO',fecha:'2026-05-01',inicio:'10:00',fin:'11:00',estado:'CONFIRMADA',modalidad:'PRESENCIAL',ubicacion:'A-1',tema:'POO',precio:0,puedeReportar:true};}
test('Valoración desde historial solo para estudiante; una vez enviada se muestra sin formulario',()=>{
 const a=ui();assert.match(a.ui.valoracionSesion({id:1}),/data-action="valorar"/);
 assert.doesNotMatch(ui('tutor','T001').ui.valoracionSesion({id:1}),/form/);
 a.state.seguimiento.misValoraciones=[{sesion:1,puntuacion:4,comentario:'<img src=x>'}];
 const html=a.ui.valoracionSesion({id:1});assert.match(html,/4\/5/);assert.doesNotMatch(html,/<img|data-action=/);assert.match(html,/&lt;img/);
});
test('Perfil separa promedio, evaluaciones y ausencia de opiniones',()=>{
 const a=ui();let t=a.state.tutores[0];assert.match(a.ui.indicadoresTutor(t),/Sin valoraciones/);
 t.cantidadValoraciones=2;t.promedioValoraciones=3;t.valoraciones=[{puntuacion:5,autor:'A',autorCarnet:'E1',sesion:1,fecha:'2026-05-02',comentario:'<script>x</script>'}];t.evaluacionesAcademicas=[{dominio:4,claridad:5,nombreCurso:'POO',curso:'C1',evaluador:'Prof',evaluadorCarnet:'P1',fecha:'2026-05-02',fundamento:'<img src=x>'}];
 const html=a.ui.indicadoresTutor(t);assert.match(html,/3\/5/);assert.match(html,/Dominio: 4\/5 · Claridad: 5\/5/);assert.doesNotMatch(html,/<script|<img/);assert.doesNotMatch(html,/data-action="evaluacion-academica"/);
});
test('Coordinación tiene acceso visible a evaluar y no evalúa su propio perfil',()=>{
 const a=ui('coordinador','P001');assert.match(a.ui.indicadoresTutor(a.state.tutores[0]),/data-action="evaluacion-academica"/);
 assert.doesNotMatch(a.ui.indicadoresTutor({...a.state.tutores[0],carnet:'P001'}),/data-action="evaluacion-academica"/);
 assert.match(a.ui.tutors(),/#perfil\?tutor=T001/);
});
test('Reporte muestra destino, motivo obligatorio y evidencia opcional sin cierre directo',()=>{
 const a=ui();const html=a.ui.formularioReporte(solicitud());assert.match(html,/María/);assert.match(html,/data-action="reportar-ausencia"/);assert.match(html,/name="motivo"[^>]*required/);assert.doesNotMatch(html,/name="evidencia"[^>]*required/);
 assert.equal(a.ui.formularioReporte({...solicitud(),puedeReportar:false}),'');
});
test('Reporte y descargos escapan texto y muestran revisión pendiente sin strikes',()=>{
 const a=ui('coordinador','P001'),r=reporte();r.explicaciones=[{autor:'T001',rolAutor:'tutor',fecha:r.fecha,contenido:'<img src=x>'}];const html=a.ui.reporteCard(r);
 assert.doesNotMatch(html,/<script|<img/);assert.match(html,/No suma strikes/);assert.match(html,/data-action="resolver-ausencia"/);assert.match(html,/data-action="explicar-ausencia"/);
});
test('Reportes decididos conservan historial y ofrecen corrección; participante no resuelve',()=>{
 const a=ui('coordinador','P001'),r=reporte();r.estado='CONFIRMADO';r.decisiones=[{estado:'CONFIRMADO',coordinador:'P001',fecha:r.fecha,motivo:'Confirmado',inicioPropuesto:'2026-05-15',finPropuesto:'2026-06-14'}];r.puedeExplicar=false;
 assert.match(a.ui.reporteCard(r),/data-action="corregir-ausencia"/);assert.doesNotMatch(a.ui.reporteCard(r),/data-action="resolver-ausencia"/);
 r.puedeResolver=false;assert.doesNotMatch(a.ui.reporteCard(r),/data-action="corregir-ausencia"/);assert.match(a.ui.reporteCard(r),/Participaste/);
});
test('Filtros por estado y semestre conservan selección cuando no hay coincidencias',()=>{
 const a=ui('coordinador','P001');a.state.seguimiento.reportes=[reporte()];const html=a.ui.ausenciasView(new URLSearchParams('estado=DESCARTADO&periodo=A'));
 assert.match(html,/value="DESCARTADO" selected/);assert.match(html,/value="A" selected/);assert.match(html,/No hay reportes que coincidan/);assert.match(html,/Limpiar filtros/);
});
test('Semestres exigen fechas reales y acuerdo explícito; no insertan un calendario supuesto',()=>{
 const a=ui('coordinador','P001');const html=a.ui.periodosView(new URLSearchParams());assert.match(html,/name="inicio" type="date" value="" required/);assert.match(html,/name="acuerdo" required/);assert.match(html,/ACUMULAR_AL_FINAL/);assert.match(html,/FECHAS_CONFIRMACION/);
});
test('Búsqueda muestra suspensión y promedio sin razones disciplinarias',()=>{
 const a=ui();a.state.tutores[0].suspendido=true;const html=a.ui.profileCards(a.state.tutores);assert.match(html,/Suspensión temporal activa/);assert.match(html,/Sin valoraciones/);assert.doesNotMatch(html,/Registro de sala/);
});
test('Sesión con reporte pendiente no permite finalizar ni cancelar, pero conserva consulta',()=>{
 const a=ui('tutor','T001');const r={...solicitud(),enRevision:true};a.state.sesiones=[{id:1,solicitud:1,finalizada:false}];const html=a.ui.requestCard(r);assert.doesNotMatch(html,/data-command="cancelar"|href="#seguimiento/);assert.match(html,/Reporte pendiente de revisión/);
});
test('Nuevas rutas son accesibles en los roles correspondientes',()=>{
 for(const rol of ['estudiante','tutor','coordinador']){const a=ui(rol);const menus=a.ui.navigation().map(n=>n[0]);assert.ok(menus.includes('ausencias'));assert.equal(menus.includes('periodos'),rol==='coordinador');a.context.location.hash='#ausencias';a.ui.render();assert.match(a.nodes.get('#app').innerHTML,/data-page="ausencias"/);}
});
