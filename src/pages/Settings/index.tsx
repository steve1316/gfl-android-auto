import Ionicons from "react-native-vector-icons/Ionicons"
import React, { useContext, useEffect, useState } from "react"
import SnackBar from "rn-snackbar-component"
import { BotStateContext } from "../../context/BotStateContext"
import { Dimensions, ScrollView, StyleSheet, View, Image } from "react-native"
import CustomCheckbox from "../../components/CustomCheckbox"
import { TextInput } from "react-native-paper"
import TitleDivider from "../../components/TitleDivider"
import { Picker } from "@react-native-picker/picker"
import { Divider, Text } from "react-native-elements"
import NumericInput from "react-native-numeric-input"
import ReactNativeZoomableView from "@openspacelabs/react-native-zoomable-view/src/ReactNativeZoomableView"
import DropDownPicker, { ValueType } from "react-native-dropdown-picker"
import mapJSON from "../../data/maps.json"
import tdollJSON from "../../data/tdolls.json"

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        margin: 10,
    },
    picker: {
        marginVertical: 10,
        backgroundColor: "azure",
    },
    dropdown: {
        marginTop: 20,
    },
})

const Settings = () => {
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false)
    const [tdolls, setTdolls] = useState<Item[]>([])
    const [corpseDragger1, setCorpseDragger1] = useState<ValueType | null>("")
    const [corpseDragger2, setCorpseDragger2] = useState<ValueType | null>("")
    const [isOpen1, setIsOpen1] = useState<boolean>(false)
    const [isOpen2, setIsOpen2] = useState<boolean>(false)
    const [firstTimeLoad, setFirstTimeLoad] = useState<boolean>(true)

    const bsc = useContext(BotStateContext)

    // TODO: Update this list each time a new map is supported for this app and is reflected both in the maps.json file and its associated map image file.
    const maps = {
        "0-2": require("../../images/0-2.webp"),
        "0-4": require("../../images/0-4.webp"),
        "2-3": require("../../images/2-3.webp"),
        "4-3e": require("../../images/4-3e.webp"),
        "5-2e": require("../../images/5-2e.webp"),
    }

    interface Item {
        label: string
        value: string
    }

    useEffect(() => {
        // Populate the dropdown picker with the list of T-Dolls.
        let newTDollList: Item[] = []
        tdollJSON.forEach((tdoll) => {
            newTDollList = newTDollList.concat({ label: tdoll.name, value: tdoll.name })
        })
        let filteredTDollList = Array.from(new Set(newTDollList))
        setTdolls(filteredTDollList)

        // Set the corpse dragger values from context.
        setCorpseDragger1(bsc.settings.gfl.corpseDragger1)
        setCorpseDragger2(bsc.settings.gfl.corpseDragger2)
        setFirstTimeLoad(false)
    }, [])

    useEffect(() => {
        // Manually set this flag to false as the snackbar autohiding does not set this to false automatically.
        setSnackbarOpen(true)
        setTimeout(() => setSnackbarOpen(false), 1500)
    }, [bsc.readyStatus])

    useEffect(() => {
        if (!firstTimeLoad) {
            bsc.setSettings({
                ...bsc.settings,
                gfl: { ...bsc.settings.gfl, corpseDragger1: corpseDragger1 !== null ? corpseDragger1.toString() : "", corpseDragger2: corpseDragger2 !== null ? corpseDragger2.toString() : "" },
            })
        }
    }, [firstTimeLoad, corpseDragger1, corpseDragger2])

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Rendering

    const renderMapSettings = () => {
        return (
            <View>
                <TitleDivider title="Map Settings" subtitle="Select the map to run." hasIcon={true} iconName="map-legend" iconColor="#000" />

                <View style={{ borderWidth: 1 }}>
                    <Picker
                        selectedValue={bsc.settings.gfl.map}
                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, map: value } })}
                        mode="dropdown"
                        style={{ color: "#000" }}
                        dropdownIconColor={"#000"}
                    >
                        {mapJSON.maps.map((map) => {
                            return <Picker.Item key={`key-${map}`} label={`${map}`} value={map} />
                        })}
                    </Picker>
                </View>

                <Text style={{ marginTop: 10, marginBottom: 10 }}>Set Number of Runs</Text>
                <NumericInput
                    type="plus-minus"
                    leftButtonBackgroundColor="#eb5056"
                    rightButtonBackgroundColor="#EA3788"
                    rounded
                    valueType="integer"
                    minValue={1}
                    maxValue={100}
                    value={bsc.settings.gfl.amount}
                    onChange={(value) => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, amount: value } })}
                    containerStyle={{ marginBottom: 10 }}
                    totalWidth={Dimensions.get("screen").width * 0.5}
                    totalHeight={50}
                />
            </View>
        )
    }

    const renderEchelonSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Echelon Settings"
                    subtitle="Select the dummy/dps echelons to deploy. Refer to the map image below for echelon placements."
                    hasIcon={true}
                    iconName="account-multiple"
                    iconColor="#000"
                />

                {bsc.settings.gfl.map !== "" ? (
                    <View style={{ height: Dimensions.get("screen").height / 3, borderWidth: 1, flexShrink: 1 }}>
                        <ReactNativeZoomableView maxZoom={2.0} minZoom={0.5} zoomStep={1.0} initialZoom={1} pinchToZoomInSensitivity={0} pinchToZoomOutSensitivity={0} movementSensibility={3}>
                            <Image style={{ width: "100%", height: "100%", resizeMode: "contain" }} source={maps[bsc.settings.gfl.map]} />
                        </ReactNativeZoomableView>
                    </View>
                ) : null}

                <Divider style={{ marginTop: 10, marginBottom: 10 }} />
                <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>The numbers signify what echelon number to deploy.</Text>
                <Divider />

                <View style={{ marginTop: 10, borderWidth: 1 }}>
                    <Text style={{ color: "#000", marginLeft: 5 }}>Dummy 1</Text>
                    <Picker
                        selectedValue={bsc.settings.gfl.dummyEchelons[0]}
                        onValueChange={(value) => {
                            const temp = bsc.settings.gfl.dummyEchelons
                            temp[0] = Number(value)
                            bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, dummyEchelons: temp } })
                        }}
                        mode="dropdown"
                        style={{ color: "#000" }}
                        dropdownIconColor={"#000"}
                    >
                        {[...Array(10 - 1 + 1).keys()]
                            .map((x) => x + 1)
                            .map((value) => {
                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                            })}
                    </Picker>
                </View>

                <View style={{ marginTop: 10, borderWidth: 1 }}>
                    <Text style={{ color: "#000", marginLeft: 5 }}>DPS 1</Text>
                    <Picker
                        selectedValue={bsc.settings.gfl.dpsEchelons[0]}
                        onValueChange={(value) => {
                            const temp = bsc.settings.gfl.dpsEchelons
                            temp[0] = Number(value)
                            bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, dpsEchelons: temp } })
                        }}
                        mode="dropdown"
                        style={{ color: "#000" }}
                        dropdownIconColor={"#000"}
                    >
                        {[...Array(10 - 1 + 1).keys()]
                            .map((x) => x + 1)
                            .map((value) => {
                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                            })}
                    </Picker>
                </View>
            </View>
        )
    }

    const renderCorpseDragSettings = () => {
        return (
            <View>
                <TitleDivider title="Corpse Drag Settings" subtitle="Customize the Corpse Drag settings." hasIcon={true} iconName="account" iconColor="#000" />

                <CustomCheckbox
                    isChecked={bsc.settings.gfl.enableCorpseDrag}
                    onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, enableCorpseDrag: !bsc.settings.gfl.enableCorpseDrag } })}
                    text="Enable Corpse Dragging"
                    subtitle="Check this to enable corpse dragging. If disabled, then bot will keep resupplying the DPS echelons and not swap out the DPS."
                />

                {bsc.settings.gfl.enableCorpseDrag ? (
                    <View>
                        <Divider style={{ marginBottom: 10 }} />
                        <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>
                            The order of corpse draggers does not matter whether it is #1 or #2. The bot will take care of it.
                        </Text>
                        <Divider />

                        <DropDownPicker
                            listMode="MODAL"
                            modalProps={{
                                animationType: "slide",
                            }}
                            style={[styles.picker, { backgroundColor: bsc.settings.gfl.corpseDragger1 !== "" ? "azure" : "pink" }]}
                            dropDownContainerStyle={styles.dropdown}
                            placeholder="Select Corpse Dragger #1"
                            searchTextInputStyle={{ fontStyle: "italic" }}
                            searchable={true}
                            items={tdolls}
                            open={isOpen1}
                            setOpen={(flag) => {
                                setIsOpen1(flag)
                            }}
                            value={corpseDragger1}
                            setValue={setCorpseDragger1}
                            zIndex={9999}
                        />

                        <CustomCheckbox
                            isChecked={bsc.settings.gfl.enableCorpseDragger1Mod}
                            onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, enableCorpseDragger1Mod: !bsc.settings.gfl.enableCorpseDragger1Mod } })}
                            text="Corpse Dragger #1 is MOD"
                            subtitle="Check this to indicate that this corpse dragger has their Digimind Upgrade."
                        />

                        <DropDownPicker
                            listMode="MODAL"
                            modalProps={{
                                animationType: "slide",
                            }}
                            style={[styles.picker, { backgroundColor: bsc.settings.gfl.corpseDragger2 !== "" ? "azure" : "pink" }]}
                            dropDownContainerStyle={styles.dropdown}
                            placeholder="Select Corpse Dragger #2"
                            searchTextInputStyle={{ fontStyle: "italic" }}
                            searchable={true}
                            items={tdolls}
                            open={isOpen2}
                            setOpen={(flag) => {
                                setIsOpen2(flag)
                            }}
                            value={corpseDragger2}
                            setValue={setCorpseDragger2}
                            zIndex={9999}
                        />

                        <CustomCheckbox
                            isChecked={bsc.settings.gfl.enableCorpseDragger2Mod}
                            onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, enableCorpseDragger2Mod: !bsc.settings.gfl.enableCorpseDragger2Mod } })}
                            text="Corpse Dragger #2 is MOD"
                            subtitle="Check this to indicate that this corpse dragger has their Digimind Upgrade."
                        />
                    </View>
                ) : null}
            </View>
        )
    }

    const renderDiscordSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Discord Settings"
                    subtitle="Please visit the wiki on the GitHub page for instructions on how to get the token and user ID."
                    hasIcon={true}
                    iconName="discord"
                    iconColor="#7289d9"
                />

                <CustomCheckbox
                    isChecked={bsc.settings.discord.enableDiscordNotifications}
                    onPress={() => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, enableDiscordNotifications: !bsc.settings.discord.enableDiscordNotifications } })}
                    text="Enable Discord Notifications"
                    subtitle="Check this to enable having the bot send you status notifications via Discord DM."
                />

                {bsc.settings.discord.enableDiscordNotifications ? (
                    <View>
                        <TextInput
                            label="Discord Token"
                            mode="outlined"
                            multiline
                            right={<TextInput.Icon name="close" onPress={() => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordToken: "" } })} />}
                            value={bsc.settings.discord.discordToken}
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordToken: value } })}
                            autoComplete={false}
                        />

                        <TextInput
                            label="Discord User ID"
                            mode="outlined"
                            multiline
                            right={<TextInput.Icon name="close" onPress={() => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordUserID: "" } })} />}
                            value={bsc.settings.discord.discordUserID}
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordUserID: value } })}
                            autoComplete={false}
                        />
                    </View>
                ) : null}
            </View>
        )
    }

    const renderRepairSettings = () => {
        return (
            <View>
                <TitleDivider title="Repair Settings" subtitle="Control the behavior of the repair functionality." hasIcon={true} iconName="hammer-wrench" iconColor="#000" />

                <CustomCheckbox
                    isChecked={bsc.settings.gfl.enableRepair}
                    onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, enableRepair: !bsc.settings.gfl.enableRepair } })}
                    text="Enable Repair"
                    subtitle="Check this to enable repairing of echelons using quick repair tickets."
                />

                {bsc.settings.gfl.enableRepair ? (
                    <View>
                        <Text style={{ marginTop: 10, marginBottom: 10 }}>Set how many runs should be completed before the bot stops and go repair the echelons</Text>
                        <NumericInput
                            type="plus-minus"
                            leftButtonBackgroundColor="#eb5056"
                            rightButtonBackgroundColor="#EA3788"
                            rounded
                            valueType="integer"
                            minValue={1}
                            maxValue={20}
                            value={bsc.settings.gfl.repairInterval}
                            onChange={(value) => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, repairInterval: value } })}
                            containerStyle={{ marginBottom: 10 }}
                            totalWidth={Dimensions.get("screen").width * 0.5}
                            totalHeight={50}
                        />
                    </View>
                ) : null}
            </View>
        )
    }

    const renderMiscSettings = () => {
        return (
            <View>
                <TitleDivider title="Misc Settings" subtitle="Below are miscelleneous settings mainly for debugging purposes." hasIcon={true} iconName="content-save-cog" iconColor="#000" />

                <CustomCheckbox
                    isChecked={bsc.settings.gfl.debugMode}
                    onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, debugMode: !bsc.settings.gfl.debugMode } })}
                    text="Enable Debug Mode"
                    subtitle="Check this to enable more detailed log messages."
                />

                <CustomCheckbox
                    isChecked={bsc.settings.gfl.enableSetup}
                    onPress={() => bsc.setSettings({ ...bsc.settings, gfl: { ...bsc.settings.gfl, enableSetup: !bsc.settings.gfl.enableSetup } })}
                    text="Enable Setup Mode"
                    subtitle="Check this to enable setting up a new map to support by following zoom instructions in the specified map's .json file and taking 5 screenshots to be saved to the /temp/ folder. It can then be used to manually determine point locations. Development purposes only."
                />
            </View>
        )
    }

    return (
        <View style={styles.root}>
            <ScrollView>
                {renderMapSettings()}

                {renderEchelonSettings()}

                {renderCorpseDragSettings()}

                {renderRepairSettings()}

                {renderMiscSettings()}

                {renderDiscordSettings()}
            </ScrollView>

            <SnackBar
                visible={snackbarOpen}
                message={bsc.readyStatus ? "Bot is ready!" : "Bot is not ready!"}
                actionHandler={() => setSnackbarOpen(false)}
                action={<Ionicons name="close" size={30} />}
                autoHidingTime={1500}
                containerStyle={{ backgroundColor: bsc.readyStatus ? "green" : "red", borderRadius: 10 }}
                native={false}
            />
        </View>
    )
}

export default Settings
