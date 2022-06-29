import Ionicons from "react-native-vector-icons/Ionicons"
import React, { useContext, useEffect, useState } from "react"
import SnackBar from "rn-snackbar-component"
import { BotStateContext } from "../../context/BotStateContext"
import { Dimensions, ScrollView, StyleSheet, View, Image } from "react-native"
import CustomCheckbox from "../../components/CustomCheckbox"
import { TextInput } from "react-native-paper"
import TitleDivider from "../../components/TitleDivider"
import { Picker } from "@react-native-picker/picker"
import { Text } from "react-native-elements"
import NumericInput from "react-native-numeric-input"
import ReactNativeZoomableView from "@openspacelabs/react-native-zoomable-view/src/ReactNativeZoomableView"
import mapJSON from "../../data/maps.json"

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        margin: 10,
    },
})

const Settings = () => {
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false)

    const bsc = useContext(BotStateContext)

    useEffect(() => {
        // Manually set this flag to false as the snackbar autohiding does not set this to false automatically.
        setSnackbarOpen(true)
        setTimeout(() => setSnackbarOpen(false), 1500)
    }, [bsc.readyStatus])

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
        // Update this list each time a new map is supported for this app and is reflected both in the maps.json file and its associated map image file.
        const maps = {
            "0-2": require("../../images/0-2.webp"),
        }

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
                    <View style={{ height: 200 }}>
                        <ReactNativeZoomableView maxZoom={2.0} minZoom={0.5} zoomStep={1.0} initialZoom={1} pinchToZoomInSensitivity={0} pinchToZoomOutSensitivity={0} movementSensibility={3}>
                            <Image style={{ width: "100%", height: "100%", resizeMode: "contain" }} source={maps[bsc.settings.gfl.map]} />
                        </ReactNativeZoomableView>
                    </View>
                ) : null}

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

    return (
        <View style={styles.root}>
            <ScrollView>
                {renderMapSettings()}

                {renderEchelonSettings()}

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
