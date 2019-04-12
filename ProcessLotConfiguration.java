package com.alphadev.processlotconfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;

import com.sap.me.common.ObjectReference;
import com.sap.me.extension.Services;
import com.sap.me.frame.SystemBase;
import com.sap.me.frame.domain.BusinessException;
import com.sap.me.frame.service.CommonMethods;
import com.sap.me.productdefinition.OperationBOHandle;
import com.sap.me.production.InvalidProcessLotException;
import com.sap.me.production.ProcessLotFullConfiguration;
import com.sap.me.production.ProcessLotServiceInterface;
import com.sap.me.production.SfcStateServiceInterface;
import com.sap.me.production.SfcStep;
import com.sap.me.production.ValidateSfcAtOperationRequest;
import com.sap.me.production.podclient.BasePodPlugin;
import com.sap.me.security.RunAsServiceLocator;
import com.sap.me.wpmf.TableConfigurator;
import com.sap.me.wpmf.util.FacesUtility;
import com.sap.tc.ls.api.enumerations.LSMessageType;
import com.sap.tc.ls.internal.faces.component.UIMessageBar;
import com.visiprise.frame.configuration.ServiceReference;

public class ProcessLotConfiguration extends BasePodPlugin {

	private static final long serialVersionUID = 1L;
	private List<ProcessLotConfigObject> processLotDetailList;
	private Boolean processLotBrowseRendered = false;
	private List<ProcessLotConfigObject> processLotBrowseList;
	private String message;
	private final SystemBase dbBase = SystemBase.createSystemBase("jdbc/jts/wipPool");
	private TableConfigurator processLotTableConfigBean;
	private ProcessLotServiceInterface processLotService;
	// private static Location logger =
	// Location.getLocation(ProcessLotConfiguration.class.getName());
	private String processLot;
	private String alertHeader;

	private Boolean tableRender = false;
	private SfcStateServiceInterface sfcStateService;

	public SfcStateServiceInterface getSfcStateService() {
		return sfcStateService;
	}

	public void setSfcStateService(SfcStateServiceInterface sfcStateService) {
		this.sfcStateService = sfcStateService;
	}

	public Boolean getTableRender() {
		return tableRender;
	}

	public void setTableRender(Boolean tableRender) {
		this.tableRender = tableRender;
	}

	public ProcessLotConfiguration() {

		super();

	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getAlertHeader() {
		return alertHeader;
	}

	public void setAlertHeader(String alertHeader) {
		this.alertHeader = alertHeader;
	}

	public String getProcessLot() {
		return processLot;
	}

	public void setProcessLot(String processLot) {
		this.processLot = processLot;
	}

	public Boolean getProcessLotBrowseRendered() {
		return processLotBrowseRendered;
	}

	public void setProcessLotBrowseRendered(Boolean processLotBrowseRendered) {
		this.processLotBrowseRendered = processLotBrowseRendered;
	}

	String ProcessLotColumnNames[] = { "SFC", "STATUS", "MATERIAL", "SHOP_ORDER", "ROUTER", "OPERATION", "STEP" };
	String ProcessLotColumnNamesDesc[] = { "sfc;Z_SFC_LABLE", "status;Z_STATUS_LABLE", "material;Z_MATERIAL_LABLE",
			"shopOrder;Z_SHOP_ORDER_LABLE", "router;Z_ROUTER_LABLE", "operation;Z_OPERATION_LABLE",
			"step;Z_STEP_LABLE" };

	public TableConfigurator getProcessLotTableConfigBean() {
		return processLotTableConfigBean;
	}

	public void setProcessLotTableConfigBean(TableConfigurator configBean) {
		this.processLotTableConfigBean = configBean;

		if (this.processLotTableConfigBean.getColumnBindings() == null
				|| this.processLotTableConfigBean.getColumnBindings().size() < 1) {

			this.processLotTableConfigBean.setListName(null);
			this.processLotTableConfigBean
					.setColumnBindings(getProcessFieldMaping(ProcessLotColumnNames, ProcessLotColumnNamesDesc));
			this.processLotTableConfigBean.setListColumnNames(ProcessLotColumnNames);
			this.processLotTableConfigBean.setAllowSelections(false);
			this.processLotTableConfigBean.setMultiSelectType(false);
			this.processLotTableConfigBean.configureTable();

		}

	}

	ProcessLotConfigObject currentProcessLotObj;
	private TableConfigurator processLotBrowseTableConfigBean = null;

	public TableConfigurator getProcessLotBrowseTableConfigBean() {
		return processLotBrowseTableConfigBean;
	}

	String processLotBrowsecolumnNames[] = { "PROCESS_LOT", "MATERIAL", "SFC_COUNT" };
	String processLotBrowsecolumnDefs[] = { "processLot;Z_PROCESS_LOT_CONFIG.processlot.text.LABEL",
			"material;Z_MATERIAL_LABLE", "sfcCount;Z_SFC_COUNT.LABLE" };

	public void setProcessLotBrowseTableConfigBean(TableConfigurator config) {
		this.processLotBrowseTableConfigBean = config;

		if (this.processLotBrowseTableConfigBean.getColumnBindings() == null
				|| this.processLotBrowseTableConfigBean.getColumnBindings().size() < 1) {

			this.processLotBrowseTableConfigBean.setListName(null);
			this.processLotBrowseTableConfigBean
					.setColumnBindings(getColumnFieldMaping(processLotBrowsecolumnDefs, processLotBrowsecolumnNames));
			this.processLotBrowseTableConfigBean.setListColumnNames(processLotBrowsecolumnNames);
			this.processLotBrowseTableConfigBean.setAllowSelections(false);
			this.processLotBrowseTableConfigBean.setMultiSelectType(false);

			this.processLotBrowseTableConfigBean.configureTable();
		}
	}

	private Map<String, String> getColumnFieldMaping(String[] processLotColumn, String[] processLotcolumnDesc) {
		HashMap<String, String> columnFieldMap = new HashMap<String, String>();
		for (int i = 0; i < processLotColumn.length; i++) {
			columnFieldMap.put(processLotcolumnDesc[i], processLotColumn[i]);
		}
		return columnFieldMap;
	}

	private Map<String, String> getProcessFieldMaping(String[] processLotColumn, String[] processLotcolumnDesc) {
		HashMap<String, String> columnFieldMap = new HashMap<String, String>();
		for (int i = 0; i < processLotColumn.length; i++) {
			columnFieldMap.put(processLotColumn[i], processLotcolumnDesc[i]);
		}
		return columnFieldMap;
	}

	public void collectProcessLot(ActionEvent event) {

		this.processLotBrowseRendered = true;

		List<ProcessLotConfigObject> procseeLotBrowse;

		procseeLotBrowse = getProcessLotList();

		if (this.processLot == null || this.processLot.equalsIgnoreCase("")) {
			this.processLotBrowseList = procseeLotBrowse;
		} else {

			this.processLotBrowseList = new ArrayList<ProcessLotConfigObject>();

			for (ProcessLotConfigObject configObject : procseeLotBrowse) {
				if (configObject.getProcessLot().startsWith(this.processLot)) {

					this.processLotBrowseList.add(configObject);
				}

			}

		}

	}

	private List<ProcessLotConfigObject> getProcessLotList() {

		List<ProcessLotConfigObject> configObjects = new ArrayList<ProcessLotConfigObject>();

		try {
			Connection con = null;
			PreparedStatement preparedStatement = null;
			String sql = "SELECT distinct PROCESS_LOT.HANDLE AS PROCESSREF , PROCESS_LOT.PROCESS_LOT, IT.ITEM AS ITEM FROM SFC "
					+ " LEFT JOIN PROCESS_LOT_MEMBER ON SFC.HANDLE =PROCESS_LOT_MEMBER.MEMBER_GBO"
					+ " LEFT JOIN SHOP_ORDER ON SFC.SHOP_ORDER_BO=SHOP_ORDER.HANDLE"
					+ " LEFT JOIN ITEM IT ON(IT.HANDLE = SHOP_ORDER.PLANNED_ITEM_BO OR SHOP_ORDER.PLANNED_ITEM_BO = 'ItemBO:'+IT.SITE+','+IT.ITEM+',#')"
					+ " RIGHT JOIN PROCESS_LOT ON PROCESS_LOT_MEMBER.PROCESS_LOT_BO=PROCESS_LOT.HANDLE";
			con = dbBase.getDBConnection();
			preparedStatement = con.prepareStatement(sql);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				ProcessLotConfigObject processLotConfigObject = new ProcessLotConfigObject();

				processLotConfigObject.setProcessLotRef(rs.getString("PROCESSREF"));
				processLotConfigObject.setProcessLot(rs.getString("PROCESS_LOT"));
				processLotConfigObject.setMaterial(rs.getString("ITEM"));

				ProcessLotFullConfiguration lotFullConfiguration = processLotService
						.readProcessLot(new ObjectReference(processLotConfigObject.getProcessLotRef()));

				int sfcCount = lotFullConfiguration.getProcessLotMemberList().size();
				if (sfcCount != 0) {
					processLotConfigObject.setSfcCount(sfcCount+"");
				}
				configObjects.add(processLotConfigObject);

			}
			rs.close();
			con.close();
			preparedStatement.close();

		} catch (SQLException e) {
			this.message = "SQL Exception" + e.getMessage();
			setMessageBar(true, LSMessageType.ERROR);
		} catch (InvalidProcessLotException e) {
			this.message = "InvalidProcessLotException" + e.getMessage();
			setMessageBar(true, LSMessageType.ERROR);
		} catch (BusinessException e) {
			this.message = "BusinessException" + e.getMessage();
			setMessageBar(true, LSMessageType.ERROR);
		}
		return configObjects;
	}

	public void processLRowSelected(ActionEvent event) {
		this.processLotBrowseRendered = false;
		this.currentProcessLotObj = getSelectedDataType();
		this.processLot = currentProcessLotObj.getProcessLot();
	}

	private ProcessLotConfigObject getSelectedDataType() {
		ProcessLotConfigObject slectedProcessLot = null;
		if (this.processLotBrowseList != null) {
			for (int i = 0; i < this.processLotBrowseList.size(); i++) {
				ProcessLotConfigObject procconfig = processLotBrowseList.get(i);

				if (procconfig != null && procconfig.getSelected()) {
					slectedProcessLot = procconfig;
					break;
				}
			}
		}
		return slectedProcessLot;
	}

	public void closeProcessLotBrowse(ActionEvent event) {
		this.processLotBrowseRendered = false;
	}

	public void retrieve(ActionEvent event) {
		this.tableRender = true;
		if (this.processLot == null || this.processLot.equalsIgnoreCase("")) {

			this.message = "Please Enter Process Lot";
			setMessageBar(true, LSMessageType.ERROR);
		} else {

			this.message = "";
			setMessageBar(false, null);

			this.processLotDetailList = getSelctedProcessLot();
			if (this.processLotDetailList == null || this.processLotDetailList.size() < 1) {

				this.message = "There is no Details for the Process Lot or Process Lot is empty";
				setMessageBar(true, LSMessageType.INFO);

			}
		}

	}

	public List<ProcessLotConfigObject> getSelctedProcessLot() {
		List<ProcessLotConfigObject> processLot = new ArrayList<ProcessLotConfigObject>();
		try {
			Connection con = null;
			PreparedStatement preparedStatement = null;

			String sql = "";

			sql = "SELECT SFC.HANDLE AS SFC_REF, IT.ITEM AS ITEM ,IT.REVISION AS ITEM_REV  ,ROUTER.REVISION AS ROUTER_REV , ROUTER_OPERATION.OPERATION_BO AS OPERATION_REF ,  SFC.SFC AS SFC ,SHOP_ORDER.SHOP_ORDER AS SHOP_ORDER, ROUTER.ROUTER AS ROUTER ,  STATUS.STATUS_DESCRIPTION AS STATUS ,ROUTER_OPERATION.OPERATION_BO AS OPERATION"
					+ " FROM SFC" + "	INNER JOIN PROCESS_LOT_MEMBER ON SFC.HANDLE =PROCESS_LOT_MEMBER.MEMBER_GBO"
					+ "	INNER JOIN SHOP_ORDER ON SFC.SHOP_ORDER_BO=SHOP_ORDER.HANDLE"
					+ "	INNER JOIN STATUS ON SFC.STATUS_BO =STATUS.HANDLE"
					+ "	INNER JOIN ROUTER_STEP ON ROUTER_STEP.ROUTER_BO= SHOP_ORDER.ROUTER_BO"
					+ "	INNER JOIN ROUTER_OPERATION ON ROUTER_STEP.HANDLE =ROUTER_OPERATION.ROUTER_STEP_BO	"
					+ "	INNER JOIN ROUTER ON SHOP_ORDER.ROUTER_BO = ROUTER.HANDLE"
					+ " INNER JOIN ITEM IT ON(IT.HANDLE = SHOP_ORDER.PLANNED_ITEM_BO OR SHOP_ORDER.PLANNED_ITEM_BO = 'ItemBO:'+IT.SITE+','+IT.ITEM+',#')"
					+ " WHERE PROCESS_LOT_MEMBER.PROCESS_LOT_BO ='" + currentProcessLotObj.getProcessLotRef() + "'";

			con = dbBase.getDBConnection();
			preparedStatement = con.prepareStatement(sql);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				ValidateSfcAtOperationRequest request = new ValidateSfcAtOperationRequest();
				request.setOperationRef(rs.getString("OPERATION_REF"));
				request.setSfcRef(rs.getString("SFC_REF"));
				Boolean response = sfcStateService.isSfcAtOperation(request);

				ObjectReference objR = new ObjectReference(rs.getString("SFC_REF"));
				ProcessLotConfigObject configObj = new ProcessLotConfigObject();
				configObj.setSfc(rs.getString("SFC"));
				configObj.setMaterial(rs.getString("ITEM") + "/" + rs.getString("ITEM_REV"));
				configObj.setRouter(rs.getString("ROUTER") + "/" + rs.getString("ROUTER_REV"));
				configObj.setStatus(rs.getString("STATUS"));
				configObj.setShopOrder(rs.getString("SHOP_ORDER"));
				configObj.setOperation(new OperationBOHandle(rs.getString("OPERATION")).getOperation());

				Collection<SfcStep> sfcSteps = sfcStateService.findCurrentRouterSfcStepsBySfcRef(objR);

				for (SfcStep sfcStep : sfcSteps) {

					// String step = sfcStep.getStatus().toString();

					configObj.setStep(sfcStep.getStepId());

				}
				if (response) {
					processLot.add(configObj);
				}

			}

			rs.close();
			con.close();
			preparedStatement.close();

		} catch (

		SQLException e) {

			this.message = "Exception occured while running Sql " + e.getMessage();
			setMessageBar(true, LSMessageType.ERROR);
		} catch (BusinessException e) {

			this.message = "SQL exception occured" + e.getMessage();
			setMessageBar(true, LSMessageType.ERROR);

		}

		return processLot;
	}

	public void setMessageBar(boolean render, LSMessageType messageType) {
		UIMessageBar messageBar = (UIMessageBar) findComponent(FacesUtility.getFacesContext().getViewRoot(),
				"processLotForm:messageBar");
		messageBar.setRendered(render);
		messageBar.setType(messageType);
		UIComponent fieldButtonPanel = findComponent(FacesUtility.getFacesContext().getViewRoot(),
				"processLotForm:processLotPanel");
		if (fieldButtonPanel != null) {
			FacesUtility.addControlUpdate(fieldButtonPanel);
		}

	}

	public List<ProcessLotConfigObject> getProcessLotBrowseList() {
		return processLotBrowseList;
	}

	public void setProcessLotBrowseList(List<ProcessLotConfigObject> processLotBrowseList) {
		this.processLotBrowseList = processLotBrowseList;
	}

	public List<ProcessLotConfigObject> getProcessLotDetailList() {
		return processLotDetailList;
	}

	public void setProcessLotDetailList(List<ProcessLotConfigObject> processLotDetailList) {
		this.processLotDetailList = processLotDetailList;
	}

	@PostConstruct
	public void init() {
		initService();
	}

	private void initService() {
		ServiceReference sfcStateServiceRef = new ServiceReference("com.sap.me.production", "SfcStateService");
		this.sfcStateService = RunAsServiceLocator.getService(sfcStateServiceRef, SfcStateServiceInterface.class,
				CommonMethods.getUserId(), CommonMethods.getSite(), null);

		this.processLotService = (ProcessLotServiceInterface) Services.getService("com.sap.me.production",
				"ProcessLotService");

	}

}
